/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.flowfunction;

import com.fluxtion.dataflow.builder.generation.context.GenerationContext;
import com.fluxtion.dataflow.runtime.context.buildtime.GeneratorNodeCollection;
import com.fluxtion.dataflow.runtime.flowfunction.LongFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.LongFlowSupplier;
import com.fluxtion.dataflow.runtime.flowfunction.TriggeredFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.AggregateLongFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.FixSizedSlidingWindow;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.TimedSlidingWindow;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.TumblingWindow.TumblingLongWindowStream;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.primitive.AggregateLongFlowFunctionWrapper;
import com.fluxtion.dataflow.runtime.flowfunction.function.BinaryMapFlowFunction.BinaryMapToLongFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.FilterDynamicFlowFunction.LongFilterDynamicFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.FilterFlowFunction.LongFilterFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.MapFlowFunction.MapLong2RefFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.MapFlowFunction.MapLong2ToDoubleFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.MapFlowFunction.MapLong2ToIntFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.MapFlowFunction.MapLong2ToLongFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.MapOnNotifyFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.NotifyFlowFunction.LongNotifyFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.PeekFlowFunction.LongPeekFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.PushFlowFunction.LongPushFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.helpers.DefaultValue;
import com.fluxtion.dataflow.runtime.flowfunction.helpers.Peekers;
import com.fluxtion.dataflow.runtime.output.SinkPublisher;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.*;

public class LongFlowBuilder implements FlowDataSupplier<LongFlowSupplier> {

    final LongFlowFunction eventStream;

    LongFlowBuilder(LongFlowFunction eventStream) {
        GenerationContext.inLineContext();
        GeneratorNodeCollection.service().add(eventStream);
        this.eventStream = eventStream;
    }

    public LongFlowSupplier flowSupplier() {
        return eventStream;
    }

    public LongFlowBuilder parallel() {
        eventStream.parallel();
        return this;
    }

    //TRIGGERS - START
    public LongFlowBuilder updateTrigger(Object updateTrigger) {
        Object source = StreamHelper.getSource(updateTrigger);
        if (eventStream instanceof TriggeredFlowFunction) {
            TriggeredFlowFunction triggeredEventStream = (TriggeredFlowFunction) eventStream;
            triggeredEventStream.setUpdateTriggerNode(source);
        }
        return this;
    }

    public LongFlowBuilder publishTrigger(Object publishTrigger) {
        Object source = StreamHelper.getSource(publishTrigger);
        if (eventStream instanceof TriggeredFlowFunction) {
            TriggeredFlowFunction triggeredEventStream = (TriggeredFlowFunction) eventStream;
            triggeredEventStream.setPublishTriggerNode(source);
        }
        return this;
    }

    public LongFlowBuilder publishTriggerOverride(Object publishTrigger) {
        Object source = StreamHelper.getSource(publishTrigger);
        if (eventStream instanceof TriggeredFlowFunction) {
            TriggeredFlowFunction triggeredEventStream = (TriggeredFlowFunction) eventStream;
            triggeredEventStream.setPublishTriggerOverrideNode(source);
        }
        return this;
    }

    public LongFlowBuilder resetTrigger(Object resetTrigger) {
        Object source = StreamHelper.getSource(resetTrigger);
        if (eventStream instanceof TriggeredFlowFunction) {
            TriggeredFlowFunction triggeredEventStream = (TriggeredFlowFunction) eventStream;
            triggeredEventStream.setResetTriggerNode(source);
        }
        return this;
    }

    public LongFlowBuilder filter(SerializableLongFunction<Boolean> filterFunction) {
        return new LongFlowBuilder(new LongFilterFlowFunction(eventStream, filterFunction));
    }

    public <S> LongFlowBuilder filter(
            SerializableBiLongPredicate predicate,
            LongFlowBuilder secondArgument) {
        return new LongFlowBuilder(
                new LongFilterDynamicFlowFunction(eventStream, secondArgument.eventStream, predicate));
    }

    public LongFlowBuilder defaultValue(long defaultValue) {
        return map(new DefaultValue.DefaultLong(defaultValue)::getOrDefault);
    }

    //PROCESSING - START
    public LongFlowBuilder map(SerializableLongUnaryOperator int2IntFunction) {
        return new LongFlowBuilder(new MapLong2ToLongFlowFunction(eventStream, int2IntFunction));
    }

    public LongFlowBuilder mapBiFunction(SerializableBiLongFunction int2IntFunction, LongFlowBuilder stream2Builder) {
        return new LongFlowBuilder(
                new BinaryMapToLongFlowFunction<>(
                        eventStream, stream2Builder.eventStream, int2IntFunction)
        );
    }

    public LongFlowBuilder mapBi(LongFlowBuilder stream2Builder, SerializableBiLongFunction int2IntFunction) {
        return mapBiFunction(int2IntFunction, stream2Builder);
    }

    public <F extends AggregateLongFlowFunction<F>> LongFlowBuilder aggregate(
            SerializableSupplier<F> aggregateFunction) {
        return new LongFlowBuilder(new AggregateLongFlowFunctionWrapper<>(eventStream, aggregateFunction));
    }

    public <F extends AggregateLongFlowFunction<F>> LongFlowBuilder tumblingAggregate(
            SerializableSupplier<F> aggregateFunction, int bucketSizeMillis) {
        return new LongFlowBuilder(
                new TumblingLongWindowStream<>(eventStream, aggregateFunction, bucketSizeMillis));
    }

    public <F extends AggregateLongFlowFunction<F>> LongFlowBuilder slidingAggregate(
            SerializableSupplier<F> aggregateFunction, int bucketSizeMillis, int numberOfBuckets) {
        return new LongFlowBuilder(
                new TimedSlidingWindow.TimedSlidingWindowLongStream<>(
                        eventStream,
                        aggregateFunction,
                        bucketSizeMillis,
                        numberOfBuckets));
    }

    public <F extends AggregateLongFlowFunction<F>> LongFlowBuilder slidingAggregateByCount(
            SerializableSupplier<F> aggregateFunction, int elementsInWindow) {
        return new LongFlowBuilder(
                new FixSizedSlidingWindow.FixSizedSlidingLongWindow<>(eventStream, aggregateFunction, elementsInWindow));
    }

    public <T> FlowBuilder<T> mapOnNotify(T target) {
        return new FlowBuilder<>(new MapOnNotifyFlowFunction<>(eventStream, target));
    }

    public FlowBuilder<Long> box() {
        return mapToObj(Long::valueOf);
    }

    public <R> FlowBuilder<R> mapToObj(LambdaReflection.SerializableLongFunction<R> int2IntFunction) {
        return new FlowBuilder<>(new MapLong2RefFlowFunction<>(eventStream, int2IntFunction));
    }

    public IntFlowBuilder mapToInt(LambdaReflection.SerializableLongToIntFunction int2IntFunction) {
        return new IntFlowBuilder(new MapLong2ToIntFlowFunction(eventStream, int2IntFunction));
    }

    public DoubleFlowBuilder mapToDouble(LambdaReflection.SerializableLongToDoubleFunction int2IntFunction) {
        return new DoubleFlowBuilder(new MapLong2ToDoubleFlowFunction(eventStream, int2IntFunction));
    }

    //OUTPUTS - START
    public LongFlowBuilder notify(Object target) {
        GeneratorNodeCollection.service().add(target);
        return new LongFlowBuilder(new LongNotifyFlowFunction(eventStream, target));
    }

    public LongFlowBuilder sink(String sinkId) {
        return push(new SinkPublisher<>(sinkId)::publishLong);
    }

    public final LongFlowBuilder push(SerializableLongConsumer... pushFunctions) {
        LongFlowBuilder target = null;
        for (SerializableLongConsumer pushFunction : pushFunctions) {
            target = new LongFlowBuilder(new LongPushFlowFunction(eventStream, pushFunction));
        }
        return target;
    }

    public LongFlowBuilder peek(LambdaReflection.SerializableConsumer<Long> peekFunction) {
        return new LongFlowBuilder(new LongPeekFlowFunction(eventStream, peekFunction));
    }

    public LongFlowBuilder console(String in) {
        peek(Peekers.console(in));
        return this;
    }

    public LongFlowBuilder console() {
        return console("{}");
    }

    //META-DATA
    public LongFlowBuilder id(String nodeId) {
        GeneratorNodeCollection.service().add(eventStream, nodeId);
        return this;
    }
}
