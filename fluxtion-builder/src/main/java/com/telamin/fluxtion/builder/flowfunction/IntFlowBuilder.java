/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.flowfunction;

import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import com.telamin.fluxtion.runtime.context.buildtime.GeneratorNodeCollection;
import com.telamin.fluxtion.runtime.flowfunction.IntFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.IntFlowSupplier;
import com.telamin.fluxtion.runtime.flowfunction.TriggeredFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateIntFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.FixSizedSlidingWindow.FixSizedSlidingIntWindow;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.TimedSlidingWindow;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.TumblingWindow.TumblingIntWindowStream;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive.AggregateIntFlowFunctionWrapper;
import com.telamin.fluxtion.runtime.flowfunction.function.BinaryMapFlowFunction.BinaryMapToIntFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.FilterDynamicFlowFunction.IntFilterDynamicFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.FilterFlowFunction.IntFilterFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.MapFlowFunction.MapInt2RefFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.MapFlowFunction.MapInt2ToDoubleFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.MapFlowFunction.MapInt2ToIntFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.MapFlowFunction.MapInt2ToLongFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.MapOnNotifyFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.NotifyFlowFunction.IntNotifyFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.PeekFlowFunction.IntPeekFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.PushFlowFunction.IntPushFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.helpers.DefaultValue;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Peekers;
import com.telamin.fluxtion.runtime.output.SinkPublisher;

import static com.telamin.fluxtion.runtime.partition.LambdaReflection.*;

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
