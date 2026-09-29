package com.telamin.fluxtion.runtime;

import com.telamin.fluxtion.runtime.annotations.runtime.ServiceRegistered;
import com.telamin.fluxtion.runtime.callback.CallbackDispatcherImpl;
import com.telamin.fluxtion.runtime.node.ObjectEventHandlerNode;
import com.telamin.fluxtion.runtime.service.Service;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

/**
 * The wedge: a node that throws must not leave the processor believing it is mid-cycle. Before the fix,
 * {@code processing} was set without a finally, so after one exception every later event was queued behind a cycle
 * that never ended — silently dropped. Now each boundary clears the flag in a finally: the exception reaches the
 * caller unchanged, and the next call dispatches normally. Nothing else of the failed cycle is completed, and what it
 * queued is discarded.
 */
public class ThrowingNodeDoesNotWedgeTest {

    static final RuntimeException BOOM = new IllegalStateException("DEMO failure");

    /** Throws on "DEMO-boom", and on stop() when asked; records everything else it handles. */
    public static class Fragile extends ObjectEventHandlerNode {
        final List<Object> seen = new ArrayList<>();
        boolean failOnStop;
        DataFlow processor;

        @Override
        protected boolean handleEvent(Object event) {
            if ("DEMO-boom".equals(event)) {
                throw BOOM;
            }
            if ("DEMO-queue-a-bomb".equals(event)) {
                processor.onEvent("DEMO-boom");                      // queued; throws when the drain reaches it
                processor.onEvent("DEMO-queued-behind-the-bomb");
                seen.add(event);
                return true;
            }
            if ("DEMO-queue-then-boom".equals(event)) {
                processor.onEvent("DEMO-queued-by-failed-cycle");   // queued: the processor is processing
                throw BOOM;
            }
            seen.add(event);
            return true;
        }

        @ServiceRegistered
        public void demoService(Runnable service) {
            throw BOOM;
        }

        @Override
        public void stop() {
            if (failOnStop) {
                throw BOOM;
            }
        }
    }

    static DefaultEventProcessor processor(Fragile node) {
        DefaultEventProcessor p = new DefaultEventProcessor(node);
        node.processor = p;
        p.init();
        p.start();
        return p;
    }

    @Test
    public void theExceptionReachesTheCallerUnchanged() {
        DefaultEventProcessor p = processor(new Fragile());
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> p.onEvent("DEMO-boom"));
        assertSame("the node's own exception, not a wrapper", BOOM, thrown);
    }

    @Test
    public void theNextEventAfterAThrowIsDispatched() {
        Fragile node = new Fragile();
        DefaultEventProcessor p = processor(node);
        assertThrows(RuntimeException.class, () -> p.onEvent("DEMO-boom"));
        p.onEvent("DEMO-after");
        p.onEvent("DEMO-later");
        assertEquals("not wedged: both later events were dispatched, not queued",
                List.of("DEMO-after", "DEMO-later"), node.seen);
    }

    @Test
    public void anEventQueuedByTheFailedCycleIsDiscarded_notDispatchedLater() {
        Fragile node = new Fragile();
        DefaultEventProcessor p = processor(node);
        assertThrows(RuntimeException.class, () -> p.onEvent("DEMO-queue-then-boom"));
        p.onEvent("DEMO-next");
        p.onEvent("DEMO-later");
        assertEquals("the failed cycle's queued event never runs, before or after the next event",
                List.of("DEMO-next", "DEMO-later"), node.seen);
    }

    @Test
    public void aQueuedEventThatThrowsDuringTheDrain_doesNotWedge_andTheRestOfTheCallIsDiscarded() throws Exception {
        Fragile node = new Fragile();
        DefaultEventProcessor p = processor(node);
        assertThrows(RuntimeException.class, () -> p.onEvent("DEMO-queue-a-bomb"));
        p.onEvent("DEMO-next");
        assertEquals("the handler ran; the bomb failed the call; what was queued behind it is discarded",
                List.of("DEMO-queue-a-bomb", "DEMO-next"), node.seen);
        // a throw inside the drain left the dispatcher's dispatching flag set; the discard clears it
        java.lang.reflect.Field dispatcherField = DefaultEventProcessor.class.getDeclaredField("callbackDispatcher");
        dispatcherField.setAccessible(true);
        java.lang.reflect.Field dispatching = CallbackDispatcherImpl.class.getDeclaredField("dispatching");
        dispatching.setAccessible(true);
        assertFalse("dispatching cleared by the discard", (boolean) dispatching.get(dispatcherField.get(p)));
    }

    @Test
    public void anExportedServiceThatThrows_doesNotWedge() {
        Fragile node = new Fragile();
        DefaultEventProcessor p = processor(node);
        Runnable demo = () -> { };
        // the registry calls the node reflectively: the node's exception arrives as the cause
        Throwable thrown = assertThrows(Throwable.class, () -> p.registerService(new Service<>(demo, Runnable.class)));
        assertSame(BOOM, thrown.getCause());
        p.onEvent("DEMO-after-service");
        assertEquals(List.of("DEMO-after-service"), node.seen);
    }

    @Test
    public void aLifecycleMethodThatThrowsDoesNotWedgeEither() {
        Fragile node = new Fragile();
        DefaultEventProcessor p = processor(node);
        node.failOnStop = true;
        assertThrows(RuntimeException.class, p::stop);
        p.onEvent("DEMO-after-stop");
        assertEquals(List.of("DEMO-after-stop"), node.seen);
    }
}
