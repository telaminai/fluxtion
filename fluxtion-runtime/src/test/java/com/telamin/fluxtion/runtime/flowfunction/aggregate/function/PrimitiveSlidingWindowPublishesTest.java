package com.telamin.fluxtion.runtime.flowfunction.aggregate.function;

import com.telamin.fluxtion.runtime.flowfunction.IntFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive.IntSumFlowFunction;
import com.telamin.fluxtion.runtime.time.Clock;
import com.telamin.fluxtion.runtime.time.ClockStrategy;
import com.telamin.fluxtion.runtime.time.FixedRateTrigger;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * A primitive timed sliding window must PROPAGATE, not merely compute.
 *
 * <p>All three primitive {@code TimedSlidingWindow} specialisations override
 * {@code timeTriggerFired} and each dropped the base class's
 * {@code publishOverrideTriggered = !overridePublishTrigger & !overrideUpdateTrigger}, so a sliding
 * window over an int, double or long flow held the right value and never published it. Downstream nodes
 * saw nothing.
 *
 * <p><b>The dispatch order is what makes it fatal, and why it hides.</b> On an event that both rolls the
 * window and carries data: {@code timeTriggerFired} sets {@code inputStreamTriggered}, then
 * {@code inputUpdated} CLEARS it while aggregating that same event's value, and {@code triggered()} then
 * reads false. {@code publishOverrideTriggered} is the separate latch the input path cannot clear. An
 * event that rolled the window WITHOUT carrying data would have worked, so only the normal case failed.
 *
 * <p>{@code TumblingWindow} has no primitive override and inherits the correct base — which is why a
 * tumbling window behaved and a sliding one did not, on the same trigger and the same data.
 *
 * <p>This asserts the ORDER, because asserting the value would pass on the broken code.
 */
public class PrimitiveSlidingWindowPublishesTest {

    /** A minimal int source the window can read. */
    static final class Source implements IntFlowFunction {
        int value;
        @Override public int getAsInt() { return value; }
        @Override public Integer get() { return value; }
        @Override public boolean hasChanged() { return true; }
        @Override public void parallel() { }
        @Override public boolean parallelCandidate() { return false; }
    }

    @Test
    public void aRollingEventThatAlsoCarriesDataStillPublishes() {
        // A data-driven clock, so the trigger's count is a function of the test rather than the
        // machine. Without driving hasExpired the count stays 0 and roll(0) never fills the ring.
        final long[] now = {0};
        Clock clock = new Clock();
        clock.init();
        clock.setClockStrategy(new ClockStrategy.ClockStrategyEvent(() -> now[0]));
        Source source = new Source();
        TimedSlidingWindow.TimedSlidingWindowIntStream<IntSumFlowFunction> window =
                new TimedSlidingWindow.TimedSlidingWindowIntStream<>(
                        source, IntSumFlowFunction::new, 2);
        window.rollTrigger = new FixedRateTrigger(clock, 10);
        window.rollTrigger.init();

        // Fill the ring: two rolls with data in between, so allBucketsFilled becomes true.
        for (int i = 0; i < 3; i++) {
            source.value = 1;
            window.inputUpdated(source);
            now[0] += 10;
            window.rollTrigger.hasExpired(new Object());
            window.timeTriggerFired(window.rollTrigger);
        }

        // The order that used to lose the publish: roll first, THEN the same event's data arrives.
        now[0] += 10;
        window.rollTrigger.hasExpired(new Object());
        window.timeTriggerFired(window.rollTrigger);
        source.value = 1;
        window.inputUpdated(source);

        assertTrue("a filled sliding window must still publish when the rolling event also carried"
                        + " data - inputUpdated clears inputStreamTriggered, and only"
                        + " publishOverrideTriggered survives it",
                window.triggered());
    }
}
