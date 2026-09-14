/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function;

import com.telamin.fluxtion.runtime.annotations.OnParentUpdate;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.flowfunction.DoubleFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.IntFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.LongFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateDoubleFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateIntFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateLongFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.AbstractFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;
import com.telamin.fluxtion.runtime.time.FixedRateTrigger;

public class TumblingWindow<T, R, S extends FlowFunction<T>, F extends AggregateFlowFunction<T, R, F>>
        extends AbstractFlowFunction<T, R, S> {

    private final SerializableSupplier<F> windowFunctionSupplier;
    protected final transient F windowFunction;
    public FixedRateTrigger rollTrigger;
    private R value;


    public TumblingWindow(S inputEventStream, SerializableSupplier<F> windowFunctionSupplier, int windowSizeMillis) {
        this(inputEventStream, windowFunctionSupplier);
        rollTrigger = FixedRateTrigger.atMillis(windowSizeMillis);
    }

    public TumblingWindow(S inputEventStream, SerializableSupplier<F> windowFunctionSupplier) {
        super(inputEventStream, null);
        this.windowFunctionSupplier = windowFunctionSupplier;
        this.windowFunction = windowFunctionSupplier.get();
    }

    @Override
    public R get() {
        return value;
    }

    protected void cacheWindowValue() {
        value = windowFunction.get();
    }

    protected void aggregateInputValue(S inputEventStream) {
        windowFunction.aggregate(inputEventStream.get());
    }

    @OnParentUpdate
    public void timeTriggerFired(FixedRateTrigger rollTrigger) {
        if (rollTrigger.getTriggerCount() == 1) {
            cacheWindowValue();
        }
        publishOverrideTriggered = !overridePublishTrigger & !overrideUpdateTrigger;
        inputStreamTriggered_1 = true;
        inputStreamTriggered = true;
        windowFunction.reset();
        if (rollTrigger.getTriggerCount() != 1) {
            cacheWindowValue();
        }
    }

    @OnParentUpdate
    public void inputUpdated(S inputEventStream) {
        aggregateInputValue(inputEventStream);
        inputStreamTriggered_1 = false;
        inputStreamTriggered = false;
    }

    @OnParentUpdate("updateTriggerNode")
    public void updateTriggerNodeUpdated(Object triggerNode) {
        super.updateTriggerNodeUpdated(triggerNode);
        cacheWindowValue();
    }

    @OnTrigger
    public boolean triggered() {
        return fireEventUpdateNotification();
    }

    @Override
    protected void resetOperation() {
        windowFunction.reset();
        rollTrigger.init();
        value = null;
    }

    @Override
    public boolean isStatefulFunction() {
        return true;
    }

    public static class TumblingIntWindowStream<F extends AggregateIntFlowFunction<F>>
            extends TumblingWindow<Integer, Integer, IntFlowFunction, F>
            implements IntFlowFunction {

        private int value;

        // This field SHADOWS TumblingWindow.value, deliberately: the specialisation exists to hold the
        // window result without boxing. The base class clears its own `value` in resetOperation(), which
        // does nothing for this one - so without the override below a reset left the PRE-RESET aggregate
        // readable through the primitive accessor, and the next window opened reporting the old window's
        // total. TimedSlidingWindow's primitive specialisations already carry this override;
        // TumblingWindow's did not.

        public TumblingIntWindowStream(IntFlowFunction inputEventStream,
                                       SerializableSupplier<F> windowFunctionSupplier,
                                       int windowSizeMillis) {
            super(inputEventStream, windowFunctionSupplier, windowSizeMillis);

        }

        public TumblingIntWindowStream(IntFlowFunction inputEventStream,
                                       SerializableSupplier<F> windowFunctionSupplier) {
            super(inputEventStream, windowFunctionSupplier);
        }

        @Override
        public int getAsInt() {
            return value;
        }

        @Override
        public Integer get() {
            return value;
        }

        protected void cacheWindowValue() {
            value = windowFunction.getAsInt();
        }

        protected void aggregateInputValue(IntFlowFunction inputEventStream) {
            windowFunction.aggregateInt(inputEventStream.getAsInt());
        }

        @Override
        protected void resetOperation() {
            // super clears windowFunction, rollTrigger and the base's shadowed `value`.
            super.resetOperation();
            // Back to the CONSTRUCTED state, which is what reset means here. Deliberately not a sentinel:
            // this 0 is the same 0 a freshly built window reports, and whether a primitive window should
            // be able to say "no window has closed yet" at all is a separate, unsettled question.
            value = 0;
        }
    }


    public static class TumblingDoubleWindowStream<F extends AggregateDoubleFlowFunction<F>>
            extends TumblingWindow<Double, Double, DoubleFlowFunction, F>
            implements DoubleFlowFunction {

        private double value;

        // This field SHADOWS TumblingWindow.value, deliberately: the specialisation exists to hold the
        // window result without boxing. The base class clears its own `value` in resetOperation(), which
        // does nothing for this one - so without the override below a reset left the PRE-RESET aggregate
        // readable through the primitive accessor, and the next window opened reporting the old window's
        // total. TimedSlidingWindow's primitive specialisations already carry this override;
        // TumblingWindow's did not.

        public TumblingDoubleWindowStream(DoubleFlowFunction inputEventStream,
                                          SerializableSupplier<F> windowFunctionSupplier,
                                          int windowSizeMillis) {
            super(inputEventStream, windowFunctionSupplier, windowSizeMillis);
        }

        public TumblingDoubleWindowStream(DoubleFlowFunction inputEventStream,
                                          SerializableSupplier<F> windowFunctionSupplier) {
            super(inputEventStream, windowFunctionSupplier);
        }

        @Override
        public double getAsDouble() {
            return value;
        }

        @Override
        public Double get() {
            return value;
        }

        protected void cacheWindowValue() {
            value = windowFunction.getAsDouble();
        }

        protected void aggregateInputValue(DoubleFlowFunction inputEventStream) {
            windowFunction.aggregateDouble(inputEventStream.getAsDouble());
        }

        @Override
        protected void resetOperation() {
            // super clears windowFunction, rollTrigger and the base's shadowed `value`.
            super.resetOperation();
            // Back to the CONSTRUCTED state, which is what reset means here. Deliberately not a sentinel:
            // this 0 is the same 0 a freshly built window reports, and whether a primitive window should
            // be able to say "no window has closed yet" at all is a separate, unsettled question.
            value = 0;
        }
    }


    public static class TumblingLongWindowStream<F extends AggregateLongFlowFunction<F>>
            extends TumblingWindow<Long, Long, LongFlowFunction, F>
            implements LongFlowFunction {

        private long value;

        // This field SHADOWS TumblingWindow.value, deliberately: the specialisation exists to hold the
        // window result without boxing. The base class clears its own `value` in resetOperation(), which
        // does nothing for this one - so without the override below a reset left the PRE-RESET aggregate
        // readable through the primitive accessor, and the next window opened reporting the old window's
        // total. TimedSlidingWindow's primitive specialisations already carry this override;
        // TumblingWindow's did not.

        public TumblingLongWindowStream(LongFlowFunction inputEventStream,
                                        SerializableSupplier<F> windowFunctionSupplier,
                                        int windowSizeMillis) {
            super(inputEventStream, windowFunctionSupplier, windowSizeMillis);
        }

        public TumblingLongWindowStream(LongFlowFunction inputEventStream,
                                        SerializableSupplier<F> windowFunctionSupplier) {
            super(inputEventStream, windowFunctionSupplier);
        }

        @Override
        public long getAsLong() {
            return value;
        }

        @Override
        public Long get() {
            return value;
        }

        protected void cacheWindowValue() {
            value = windowFunction.getAsLong();
        }

        protected void aggregateInputValue(LongFlowFunction inputEventStream) {
            windowFunction.aggregateLong(inputEventStream.getAsLong());
        }

        @Override
        protected void resetOperation() {
            // super clears windowFunction, rollTrigger and the base's shadowed `value`.
            super.resetOperation();
            // Back to the CONSTRUCTED state, which is what reset means here. Deliberately not a sentinel:
            // this 0 is the same 0 a freshly built window reports, and whether a primitive window should
            // be able to say "no window has closed yet" at all is a separate, unsettled question.
            value = 0;
        }

    }
}
