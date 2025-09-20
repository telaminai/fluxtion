/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.flowfunction;

import com.fluxtion.dataflow.builder.generation.context.GenerationContext;
import com.fluxtion.dataflow.runtime.context.buildtime.GeneratorNodeCollection;
import com.fluxtion.dataflow.runtime.flowfunction.IntFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.IntFlowSupplier;
import com.fluxtion.dataflow.runtime.flowfunction.TriggeredFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.AggregateIntFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.FixSizedSlidingWindow.FixSizedSlidingIntWindow;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.TimedSlidingWindow;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.TumblingWindow.TumblingIntWindowStream;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.primitive.AggregateIntFlowFunctionWrapper;
import com.fluxtion.dataflow.runtime.flowfunction.function.BinaryMapFlowFunction.BinaryMapToIntFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.FilterDynamicFlowFunction.IntFilterDynamicFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.FilterFlowFunction.IntFilterFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.MapFlowFunction.MapInt2RefFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.MapFlowFunction.MapInt2ToDoubleFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.MapFlowFunction.MapInt2ToIntFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.MapFlowFunction.MapInt2ToLongFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.MapOnNotifyFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.NotifyFlowFunction.IntNotifyFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.PeekFlowFunction.IntPeekFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.PushFlowFunction.IntPushFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.helpers.DefaultValue;
import com.fluxtion.dataflow.runtime.flowfunction.helpers.Peekers;
import com.fluxtion.dataflow.runtime.output.SinkPublisher;

import static com.fluxtion.dataflow.runtime.partition.LambdaReflection.*;

public class IntFlowBuilder implements FlowDataSupplier<IntFlowSupplier> {

    final IntFlowFunction eventStream;

    IntFlowBuilder(IntFlowFunction eventStream) {
        GenerationContext.inLineContext();
        GeneratorNodeCollection.service().add(eventStream);
        this.eventStream = eventStream;
    }

    @Override
    public IntFlowSupplier flowSupplier() {
        return eventStream;
    }

    public IntFlowBuilder parallel() {
        eventStream.parallel();
        return this;
    }

    //TRIGGERS - START
    public IntFlowBuilder updateTrigger(Object updateTrigger) {
        Object source = StreamHelper.getSource(updateTrigger);
        if (eventStream instanceof TriggeredFlowFunction) {
            TriggeredFlowFunction triggeredEventStream = (TriggeredFlowFunction) eventStream;
            triggeredEventStream.setUpdateTriggerNode(source);
        }
        return this;
    }

    public IntFlowBuilder publishTrigger(Object publishTrigger) {
        Object source = StreamHelper.getSource(publishTrigger);
        if (eventStream instanceof TriggeredFlowFunction) {
            TriggeredFlowFunction triggeredEventStream = (TriggeredFlowFunction) eventStream;
            triggeredEventStream.setPublishTriggerNode(source);
        }
        return this;
    }

    public IntFlowBuilder publishTriggerOverride(Object publishTrigger) {
        Object source = StreamHelper.getSource(publishTrigger);
        if (eventStream instanceof TriggeredFlowFunction) {
            TriggeredFlowFunction triggeredEventStream = (TriggeredFlowFunction) eventStream;
            triggeredEventStream.setPublishTriggerOverrideNode(source);
        }
        return this;
    }

    public IntFlowBuilder resetTrigger(Object resetTrigger) {
        Object source = StreamHelper.getSource(resetTrigger);
        if (eventStream instanceof TriggeredFlowFunction) {
            TriggeredFlowFunction triggeredEventStream = (TriggeredFlowFunction) eventStream;
            triggeredEventStream.setResetTriggerNode(source);
        }
        return this;
    }

    public IntFlowBuilder filter(SerializableIntFunction<Boolean> filterFunction) {
        return new IntFlowBuilder(new IntFilterFlowFunction(eventStream, filterFunction));
    }

    public <S> IntFlowBuilder filter(
            SerializableBiIntPredicate predicate,
            IntFlowBuilder secondArgument) {
        return new IntFlowBuilder(
                new IntFilterDynamicFlowFunction(eventStream, secondArgument.eventStream, predicate));
    }

    public IntFlowBuilder defaultValue(int defaultValue) {
        return map(new DefaultValue.DefaultInt(defaultValue)::getOrDefault);
    }

    //PROCESSING - START
    public IntFlowBuilder map(SerializableIntUnaryOperator int2IntFunction) {
        return new IntFlowBuilder(new MapInt2ToIntFlowFunction(eventStream, int2IntFunction));
    }

    public IntFlowBuilder mapBiFunction(SerializableBiIntFunction int2IntFunction, IntFlowBuilder stream2Builder) {
        return new IntFlowBuilder(
                new BinaryMapToIntFlowFunction<>(
                        eventStream, stream2Builder.eventStream, int2IntFunction)
        );
    }

    public IntFlowBuilder mapBi(IntFlowBuilder stream2Builder, SerializableBiIntFunction int2IntFunction) {
        return mapBiFunction(int2IntFunction, stream2Builder);
    }

    public <F extends AggregateIntFlowFunction<F>> IntFlowBuilder aggregate(
            SerializableSupplier<F> aggregateFunction) {
        return new IntFlowBuilder(new AggregateIntFlowFunctionWrapper<>(eventStream, aggregateFunction));
    }

    public <F extends AggregateIntFlowFunction<F>> IntFlowBuilder tumblingAggregate(
            SerializableSupplier<F> aggregateFunction, int bucketSizeMillis) {
        return new IntFlowBuilder(
                new TumblingIntWindowStream<>(eventStream, aggregateFunction, bucketSizeMillis));
    }

    public <F extends AggregateIntFlowFunction<F>> IntFlowBuilder slidingAggregate(
            SerializableSupplier<F> aggregateFunction, int bucketSizeMillis, int numberOfBuckets) {
        return new IntFlowBuilder(
                new TimedSlidingWindow.TimedSlidingWindowIntStream<>(
                        eventStream,
                        aggregateFunction,
                        bucketSizeMillis,
                        numberOfBuckets));
    }

    public <F extends AggregateIntFlowFunction<F>> IntFlowBuilder slidingAggregateByCount(
            SerializableSupplier<F> aggregateFunction, int elementsInWindow) {
        return new IntFlowBuilder(
                new FixSizedSlidingIntWindow<>(eventStream, aggregateFunction, elementsInWindow));
    }

    public <T> FlowBuilder<T> mapOnNotify(T target) {
        return new FlowBuilder<>(new MapOnNotifyFlowFunction<>(eventStream, target));
    }

    public FlowBuilder<Integer> box() {
        return mapToObj(Integer::valueOf);
    }

    public <R> FlowBuilder<R> mapToObj(SerializableIntFunction<R> int2IntFunction) {
        return new FlowBuilder<>(new MapInt2RefFlowFunction<>(eventStream, int2IntFunction));
    }

    public DoubleFlowBuilder mapToDouble(SerializableIntToDoubleFunction int2IntFunction) {
        return new DoubleFlowBuilder(new MapInt2ToDoubleFlowFunction(eventStream, int2IntFunction));
    }

    public LongFlowBuilder mapToLong(SerializableIntToLongFunction int2IntFunction) {
        return new LongFlowBuilder(new MapInt2ToLongFlowFunction(eventStream, int2IntFunction));
    }

    //OUTPUTS - START
    public IntFlowBuilder notify(Object target) {
        GeneratorNodeCollection.service().add(target);
        return new IntFlowBuilder(new IntNotifyFlowFunction(eventStream, target));
    }

    public IntFlowBuilder sink(String sinkId) {
        return push(new SinkPublisher<>(sinkId)::publishInt);
    }

    public final IntFlowBuilder push(SerializableIntConsumer... pushFunctions) {
        IntFlowBuilder target = null;
        for (SerializableIntConsumer pushFunction : pushFunctions) {
            target = new IntFlowBuilder(new IntPushFlowFunction(eventStream, pushFunction));
        }
        return target;
    }

    public IntFlowBuilder peek(SerializableConsumer<Integer> peekFunction) {
        return new IntFlowBuilder(new IntPeekFlowFunction(eventStream, peekFunction));
    }

    public IntFlowBuilder console(String in) {
        peek(Peekers.console(in));
        return this;
    }

    public IntFlowBuilder console() {
        return console("{}");
    }

    //META-DATA
    public IntFlowBuilder id(String nodeId) {
        GeneratorNodeCollection.service().add(eventStream, nodeId);
        return this;
    }
}
