package com.telamin.fluxtion.runtime;

import com.telamin.fluxtion.runtime.event.Event;
import com.telamin.fluxtion.runtime.node.ObjectEventHandlerNode;
import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The clock inside a {@link DataFlow#runInEventCycle} cycle on {@link DefaultEventProcessor}. An audit context that is
 * an {@link Event} supplies its own event time, as it does on the event path ({@code Clock.eventReceived(Event)}); any
 * other context takes the process time. The process time is a reading of the clock strategy either way, and the
 * context reaches no node.
 */
public class RunInEventCycleClockTest {

    static final long STRATEGY_TIME = 42;
    static final long CONTEXT_EVENT_TIME = 17;

    /** An audit context that states its own event time, different from the clock strategy's reading. */
    static class TimedContext implements Event {
        @Override
        public long getEventTime() {
            return CONTEXT_EVENT_TIME;
        }

        @Override
        public String toString() {
            return "TimedContext[DEMO]";
        }
    }

    /** An ordinary audit context: not an Event. */
    static class PlainContext {
        @Override
        public String toString() {
            return "PlainContext[DEMO]";
        }
    }

    static class Recorder extends ObjectEventHandlerNode {
        final List<Object> seen = new ArrayList<>();

        @Override
        protected boolean handleEvent(Object event) {
            seen.add(event);
            return true;
        }
    }

    private final Recorder node = new Recorder();
    private final DefaultEventProcessor processor = new DefaultEventProcessor(node);

    private long[] timesInside(Object context) throws Exception {
        processor.init();
        processor.start();
        processor.setClockStrategy(() -> STRATEGY_TIME);
        Clock clock = processor.getAuditorById("clock");
        long[] times = {Long.MIN_VALUE, Long.MIN_VALUE};
        processor.runInEventCycle(context, () -> {
            times[0] = clock.getEventTime();
            times[1] = clock.getProcessTime();
        });
        return times;
    }

    private void assertReachedNoNode(Class<?> contextClass) {
        assertTrue("the audit context is dispatched to no node: " + node.seen,
                node.seen.stream().noneMatch(contextClass::isInstance));
    }

    @Test
    public void anEventContextSuppliesItsOwnEventTime() throws Exception {
        long[] times = timesInside(new TimedContext());
        assertEquals("eventTime is the Event's own", CONTEXT_EVENT_TIME, times[0]);
        assertEquals("processTime is the strategy's reading", STRATEGY_TIME, times[1]);
        assertReachedNoNode(TimedContext.class);
    }

    @Test
    public void anOrdinaryContextTakesTheProcessTime() throws Exception {
        long[] times = timesInside(new PlainContext());
        assertEquals("eventTime falls back to the process time", STRATEGY_TIME, times[0]);
        assertEquals("processTime is the strategy's reading", STRATEGY_TIME, times[1]);
        assertReachedNoNode(PlainContext.class);
    }
}
