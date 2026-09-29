package com.telamin.fluxtion.runtime;

import com.telamin.fluxtion.runtime.node.ObjectEventHandlerNode;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * DataFlow.runInEventCycle on the hand-written DefaultEventProcessor: the action runs inside a cycle, an event it
 * raises is queued and dispatched after it, a throwing action leaves the processor as it found it, and a call inside a
 * cycle is refused. (The audit record is shown on generated processors, which have an EventLogManager.)
 */
public class RunInEventCycleTest {

    /** Notes what it handles, and whether the processor was mid-action when it did. */
    static class Recorder extends ObjectEventHandlerNode {
        final List<String> seen = new ArrayList<>();
        DataFlow processor;
        boolean inAction;

        @Override
        protected boolean handleEvent(Object event) {
            seen.add(event + (inAction ? " (during the action)" : ""));
            return true;
        }
    }

    static DefaultEventProcessor processor(Recorder node) {
        DefaultEventProcessor p = new DefaultEventProcessor(node);
        node.processor = p;
        p.init();
        p.start();
        return p;
    }

    @Test
    public void anEventTheActionRaisesIsQueued_andDispatchedAfterIt() {
        Recorder node = new Recorder();
        DefaultEventProcessor p = processor(node);
        List<String> order = new ArrayList<>();
        p.runInEventCycle("DEMO-command", () -> {
            node.inAction = true;
            order.add("action begins");
            p.onEvent("DEMO-raised");                   // queued: the processor is processing
            order.add("action ends");
            node.inAction = false;
        });
        assertEquals(List.of("action begins", "action ends"), order);
        assertEquals("the raised event ran after the action, as its own cycle, not inside it",
                List.of("DEMO-raised"), node.seen);
        // and the processor is idle again: the next event dispatches at once
        p.onEvent("DEMO-next");
        assertEquals(List.of("DEMO-raised", "DEMO-next"), node.seen);
    }

    @Test
    public void theAuditEventIsNotDispatchedToAnyNode() {
        Recorder node = new Recorder();
        DefaultEventProcessor p = processor(node);
        p.runInEventCycle("DEMO-command", () -> { });
        assertTrue("the audit event is the cycle's context, not an input: " + node.seen, node.seen.isEmpty());
    }

    @Test
    public void aThrowingActionLeavesTheProcessorAsItFoundIt() {
        Recorder node = new Recorder();
        DefaultEventProcessor p = processor(node);
        assertThrows(IllegalStateException.class, () -> p.runInEventCycle("DEMO-command", () -> {
            throw new IllegalStateException("DEMO failure");
        }));
        p.onEvent("DEMO-after");
        assertEquals("not wedged: the next event is processed", List.of("DEMO-after"), node.seen);
    }

    @Test
    public void anEventAThrowingActionRaisedIsDiscarded() {
        Recorder node = new Recorder();
        DefaultEventProcessor p = processor(node);
        assertThrows(IllegalStateException.class, () -> p.runInEventCycle("DEMO-command", () -> {
            p.onEvent("DEMO-raised-by-failed-action");      // queued
            throw new IllegalStateException("DEMO failure");
        }));
        p.onEvent("DEMO-after");
        assertEquals("a failed action's queued event is discarded, not dispatched", List.of("DEMO-after"), node.seen);
    }

    @Test
    public void aCallInsideACycleIsRefused() {
        Recorder node = new Recorder();
        DefaultEventProcessor p = processor(node);
        List<Throwable> refused = new ArrayList<>();
        p.runInEventCycle("DEMO-outer", () -> {
            try {
                p.runInEventCycle("DEMO-inner", () -> { });
            } catch (IllegalStateException e) {
                refused.add(e);
            }
        });
        assertEquals(1, refused.size());
        assertTrue(refused.get(0).getMessage(), refused.get(0).getMessage().contains("not re-entrant"));
    }

    @Test
    public void theDefaultRunsTheActionAsAnOlderProcessorWould() {
        List<String> ran = new ArrayList<>();
        DataFlow old = new DataFlow() {
            @Override
            public void onEvent(Object event) {
            }

            @Override
            public void init() {
            }

            @Override
            public void tearDown() {
            }
        };
        old.runInEventCycle("DEMO-command", () -> ran.add("ran"));
        assertEquals(List.of("ran"), ran);
        assertFalse(ran.isEmpty());
    }
}
