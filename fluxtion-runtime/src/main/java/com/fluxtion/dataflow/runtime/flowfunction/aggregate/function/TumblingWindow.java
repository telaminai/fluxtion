/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function;

import com.fluxtion.dataflow.runtime.annotations.OnParentUpdate;
import com.fluxtion.dataflow.runtime.annotations.OnTrigger;
import com.fluxtion.dataflow.runtime.flowfunction.DoubleFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.FlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.IntFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.LongFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.AggregateDoubleFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.AggregateFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.AggregateIntFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.AggregateLongFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.AbstractFlowFunction;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableSupplier;
import com.fluxtion.dataflow.runtime.time.FixedRateTrigger;

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
    }


    public static class TumblingDoubleWindowStream<F extends AggregateDoubleFlowFunction<F>>
            extends TumblingWindow<Double, Double, DoubleFlowFunction, F>
            implements DoubleFlowFunction {

        private double value;

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
    }


    public static class TumblingLongWindowStream<F extends AggregateLongFlowFunction<F>>
            extends TumblingWindow<Long, Long, LongFlowFunction, F>
            implements LongFlowFunction {

        private long value;

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

    }
}
