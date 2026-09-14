package com.telamin.fluxtion.runtime.flowfunction.aggregate.function;

import com.telamin.fluxtion.runtime.flowfunction.DoubleFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.IntFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.LongFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive.DoubleSumFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive.IntSumFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive.LongSumFlowFunction;
import com.telamin.fluxtion.runtime.time.Clock;
import com.telamin.fluxtion.runtime.time.ClockStrategy;
import com.telamin.fluxtion.runtime.time.FixedRateTrigger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * A reset must clear a primitive tumbling window's cached result.
 *
 * <p>{@code TumblingIntWindowStream}, {@code TumblingDoubleWindowStream} and
 * {@code TumblingLongWindowStream} each declare their own primitive {@code value}, which <b>shadows</b>
 * {@code TumblingWindow.value}. The shadowing is deliberate — it is how the specialisation holds a
 * window result without boxing — but {@code resetOperation()} lived only on the base class and cleared
 * only the base class's field. None of the three overrode it.
 *
 * <p>So a reset cleared {@code windowFunction} and {@code rollTrigger}, and left the finished window's
 * total still readable through {@code getAsInt()} / {@code getAsDouble()} / {@code getAsLong()}. The
 * next window opened reporting the previous window's number.
 *
 * <p>{@code TimedSlidingWindow}'s three primitive specialisations already carried this override, which
 * is why only the tumbling family was affected — the same asymmetry, in the opposite direction, as the
 * publish defect recorded in {@link PrimitiveSlidingWindowPublishesTest}.
 *
 * <p><b>Each case asserts the pre-reset value is non-zero before asserting the post-reset value is
 * zero.</b> Without that guard every one of these tests would pass on a window that never aggregated
 * anything, which is exactly the failure mode the fix is about.
 */
public class PrimitiveTumblingWindowResetTest {

    static final class IntSource implements IntFlowFunction {
        int value;
        @Override public int getAsInt() { return value; }
        @Override public Integer get() { return value; }
        @Override public boolean hasChanged() { return true; }
        @Override public void parallel() { }
        @Override public boolean parallelCandidate() { return false; }
    }

    static final class DoubleSource implements DoubleFlowFunction {
        double value;
        @Override public double getAsDouble() { return value; }
        @Override public Double get() { return value; }
        @Override public boolean hasChanged() { return true; }
        @Override public void parallel() { }
        @Override public boolean parallelCandidate() { return false; }
    }

    static final class LongSource implements LongFlowFunction {
        long value;
        @Override public long getAsLong() { return value; }
        @Override public Long get() { return value; }
        @Override public boolean hasChanged() { return true; }
        @Override public void parallel() { }
        @Override public boolean parallelCandidate() { return false; }
    }

    /** A clock the test drives, so the trigger count is a function of the test and not the machine. */
    private static Clock dataDrivenClock(long[] now) {
        Clock clock = new Clock();
        clock.init();
        clock.setClockStrategy(new ClockStrategy.ClockStrategyEvent(() -> now[0]));
        return clock;
    }

    @Test
    public void aResetClearsTheCachedWindowValue_int() {
        final long[] now = {0};
        IntSource source = new IntSource();
        TumblingWindow.TumblingIntWindowStream<IntSumFlowFunction> window =
                new TumblingWindow.TumblingIntWindowStream<>(source, IntSumFlowFunction::new, 10);
        window.rollTrigger = new FixedRateTrigger(dataDrivenClock(now), 10);
        window.rollTrigger.init();

        source.value = 7;
        window.inputUpdated(source);
        now[0] += 10;
        window.rollTrigger.hasExpired(new Object());
        window.timeTriggerFired(window.rollTrigger);

        assertNotEquals("the window must hold a real total before the reset is meaningful",
                0, window.getAsInt());

        window.resetTriggerNodeUpdated(new Object());

        assertEquals("a reset must clear the primitive window's own cached value, not just the base"
                + " class's shadowed one", 0, window.getAsInt());
        assertEquals("the boxed accessor reads the same field", Integer.valueOf(0), window.get());
    }

    @Test
    public void aResetClearsTheCachedWindowValue_double() {
        final long[] now = {0};
        DoubleSource source = new DoubleSource();
        TumblingWindow.TumblingDoubleWindowStream<DoubleSumFlowFunction> window =
                new TumblingWindow.TumblingDoubleWindowStream<>(source, DoubleSumFlowFunction::new, 10);
        window.rollTrigger = new FixedRateTrigger(dataDrivenClock(now), 10);
        window.rollTrigger.init();

        source.value = 7.5;
        window.inputUpdated(source);
        now[0] += 10;
        window.rollTrigger.hasExpired(new Object());
        window.timeTriggerFired(window.rollTrigger);

        assertNotEquals("the window must hold a real total before the reset is meaningful",
                0.0, window.getAsDouble(), 0.0);

        window.resetTriggerNodeUpdated(new Object());

        assertEquals("a reset must clear the primitive window's own cached value, not just the base"
                + " class's shadowed one", 0.0, window.getAsDouble(), 0.0);
    }

    @Test
    public void aResetClearsTheCachedWindowValue_long() {
        final long[] now = {0};
        LongSource source = new LongSource();
        TumblingWindow.TumblingLongWindowStream<LongSumFlowFunction> window =
                new TumblingWindow.TumblingLongWindowStream<>(source, LongSumFlowFunction::new, 10);
        window.rollTrigger = new FixedRateTrigger(dataDrivenClock(now), 10);
        window.rollTrigger.init();

        source.value = 7;
        window.inputUpdated(source);
        now[0] += 10;
        window.rollTrigger.hasExpired(new Object());
        window.timeTriggerFired(window.rollTrigger);

        assertNotEquals("the window must hold a real total before the reset is meaningful",
                0L, window.getAsLong());

        window.resetTriggerNodeUpdated(new Object());

        assertEquals("a reset must clear the primitive window's own cached value, not just the base"
                + " class's shadowed one", 0L, window.getAsLong());
    }
}
