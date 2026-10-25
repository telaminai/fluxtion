package com.telamin.fluxtion.runtime;

import com.telamin.fluxtion.runtime.callback.CallBackNode;
import com.telamin.fluxtion.runtime.callback.CallbackDispatcherImpl;
import com.telamin.fluxtion.runtime.callback.InstanceCallbackEvent;
import com.telamin.fluxtion.runtime.callback.InternalEventProcessor;
import com.telamin.fluxtion.runtime.node.ObjectEventHandlerNode;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

/**
 * Recovery after a failed cycle on the runtime's own processor (telaminai/fluxtion#36, telaminai/fluxtion-compiler#105):
 * buffered dispatch, a throw while runInEventCycle drains, and callback payloads. Each asserts the exact values the next
 * successful cycle delivers. The buffered case began as the #36 review probe ({@code Review36RuntimeProbeTest}).
 */
public class FailedCycleRecoveryTest {

    static class Node extends ObjectEventHandlerNode {
        final List<Object> seen = new ArrayList<>();
        CallbackDispatcherImpl dispatcher;
        DataFlow processor;

        @Override
        protected boolean handleEvent(Object event) {
            if ("DEMO-fail".equals(event)) {
                dispatcher.processReentrantEvent("DEMO-stale");
                throw new IllegalStateException("DEMO-fail");
            }
            if ("DEMO-poison".equals(event)) {
                throw new IllegalStateException("DEMO-poison");
            }
            seen.add(event);
            return true;
        }
    }

    static DefaultEventProcessor processor(Node n) throws Exception {
        DefaultEventProcessor p = new DefaultEventProcessor(n);
        Field f = DefaultEventProcessor.class.getDeclaredField("callbackDispatcher");
        f.setAccessible(true);
        n.dispatcher = (CallbackDispatcherImpl) f.get(p);
        n.processor = p;
        p.init();
        p.start();
        return p;
    }

    @Test
    public void bufferedFailureDiscardsQueuedWork() throws Exception {
        Node n = new Node();
        DefaultEventProcessor p = processor(n);
        assertThrows(IllegalStateException.class, () -> p.bufferEvent("DEMO-fail"));
        p.onEvent("DEMO-fresh");
        assertEquals(List.of("DEMO-fresh"), n.seen);
    }

    @Test
    public void bufferedFailureEndsTheBufferedBatch_laterBufferingStillWorks() throws Exception {
        Node n = new Node();
        DefaultEventProcessor p = processor(n);
        assertThrows(IllegalStateException.class, () -> p.bufferEvent("DEMO-fail"));
        p.bufferEvent("DEMO-buffered");
        p.triggerCalculation();
        p.onEvent("DEMO-next");
        assertEquals(List.of("DEMO-buffered", "DEMO-next"), n.seen);
    }

    @Test
    public void runInEventCycle_aQueuedEventThatThrowsWhileDraining_doesNotLeakIntoTheNextEvent() throws Exception {
        Node n = new Node();
        DefaultEventProcessor p = processor(n);
        RuntimeException thrown = assertThrows(IllegalStateException.class, () -> p.runInEventCycle("DEMO-cmd", () -> {
            p.onEvent("DEMO-poison");                 // queued: the cycle is open
            p.onEvent("DEMO-queued-behind-poison");   // queued behind it: discarded with it
        }));
        assertEquals("DEMO-poison", thrown.getMessage());
        p.onEvent("DEMO-next");                       // must neither rethrow DEMO-poison nor deliver the discarded event
        assertEquals(List.of("DEMO-next"), n.seen);
    }

    @Test
    public void runInEventCycle_aFailedActionKeepsItsOwnException() throws Exception {
        Node n = new Node();
        DefaultEventProcessor p = processor(n);
        IllegalArgumentException boom = new IllegalArgumentException("DEMO-action");
        Throwable thrown = assertThrows(Throwable.class, () -> p.runInEventCycle("DEMO-cmd", () -> {
            p.onEvent("DEMO-poison");                 // queued, then discarded with the failed action
            throw boom;
        }));
        assertSame(boom, thrown);
        p.onEvent("DEMO-next");
        assertEquals(List.of("DEMO-next"), n.seen);
    }

    /** #36 item 1: the payload of a discarded callback is discarded with it (PR #35 review sequence). */
    @Test
    public void aDiscardedCallbackPayloadIsNotDeliveredToTheNextCallback() throws Exception {
        CallbackDispatcherImpl dispatcher = new CallbackDispatcherImpl();
        CallBackNode<String> callback = new CallBackNode<>(new InstanceCallbackEvent.InstanceCallbackEvent_0());
        Field f = CallBackNode.class.getDeclaredField("dispatcher");
        f.setAccessible(true);
        f.set(callback, dispatcher);
        List<String> got = new ArrayList<>();
        dispatcher.setEventProcessor(recording(callback, got));
        callback.fireCallback("DEMO-stale");
        dispatcher.discardQueuedCallbacks();
        callback.fireCallback("DEMO-fresh");
        dispatcher.dispatchQueuedCallbacks();
        assertEquals(List.of("DEMO-fresh"), got);
    }

    /** A callback node reused by a second processor: a failure in the first must not clear the second's payloads. */
    @Test
    public void aReusedCallbackNodeMovesItsRegistration_theOldProcessorsDiscardLeavesTheNewPayload() throws Exception {
        CallbackDispatcherImpl first = new CallbackDispatcherImpl();
        CallbackDispatcherImpl second = new CallbackDispatcherImpl();
        CallBackNode<String> callback = new CallBackNode<>(new InstanceCallbackEvent.InstanceCallbackEvent_1());
        Field f = CallBackNode.class.getDeclaredField("dispatcher");
        f.setAccessible(true);
        List<String> got = new ArrayList<>();
        first.setEventProcessor(recording(callback, new ArrayList<>()));
        second.setEventProcessor(recording(callback, got));
        f.set(callback, first);
        callback.fireCallback("DEMO-first");
        first.dispatchQueuedCallbacks();
        f.set(callback, second);                     // reused by another processor
        callback.fireCallback("DEMO-second");
        first.discardQueuedCallbacks();              // the old processor fails
        second.dispatchQueuedCallbacks();
        assertEquals(List.of("DEMO-second"), got);
    }

    private static InternalEventProcessor recording(CallBackNode<String> callback, List<String> got) {
        return new InternalEventProcessor() {
            @Override
            public void onEvent(Object event) {
                onEventInternal(event);
            }

            @Override
            public void onEventInternal(Object event) {
                callback.onEvent((InstanceCallbackEvent) event);
                got.add(callback.get());
            }

            @Override
            public void bufferEvent(Object event) {
            }

            @Override
            public boolean isDirty(Object node) {
                return false;
            }

            @Override
            public BooleanSupplier dirtySupplier(Object node) {
                return () -> false;
            }

            @Override
            public void setDirty(Object node, boolean dirtyFlag) {
            }

            @Override
            public <T> T getNodeById(String id) {
                return null;
            }
        };
    }
}
