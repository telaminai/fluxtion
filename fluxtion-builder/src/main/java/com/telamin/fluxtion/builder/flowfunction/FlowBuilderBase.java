/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.flowfunction;

import com.telamin.fluxtion.runtime.context.buildtime.GeneratorNodeCollection;
import com.telamin.fluxtion.runtime.flowfunction.FlowSupplier;
import com.telamin.fluxtion.runtime.flowfunction.TriggeredFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.AggregateFlowFunctionWrapper;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.TimedSlidingWindow;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.TumblingWindow;
import com.telamin.fluxtion.runtime.flowfunction.function.BinaryMapFlowFunction.BinaryMapToRefFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.MapFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.MapFlowFunction.MapRef2RefFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.MergeFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupByFlowFunctionWrapper;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupByTimedSlidingWindow;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupByTumblingWindow;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Aggregates;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Collectors;
import com.telamin.fluxtion.runtime.flowfunction.helpers.DefaultValue;
import com.telamin.fluxtion.runtime.flowfunction.helpers.DefaultValue.DefaultValueFromSupplier;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Mappers;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableBiFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;

import java.util.List;

public class FlowBuilderBase<T> extends AbstractFlowBuilder<T, FlowBuilderBase<T>> implements FlowDataSupplier<FlowSupplier<T>> {


    FlowBuilderBase(TriggeredFlowFunction<T> eventStream) {
        super(eventStream);
        GeneratorNodeCollection.service().add(eventStream);
    }

    @Override
    protected FlowBuilderBase<T> connect(TriggeredFlowFunction<T> stream) {
        return new FlowBuilderBase<>(stream);
    }


    @Override
    protected <R> FlowBuilderBase<R> connectMap(TriggeredFlowFunction<R> stream) {
        return new FlowBuilderBase<>(stream);
    }


    @Override
    protected FlowBuilderBase<T> identity() {
        return this;
    }

    public FlowSupplier<T> flowSupplier() {
        return GeneratorNodeCollection.service().add(eventStream);
    }

    public FlowBuilderBase<T> defaultValue(T defaultValue) {
        return map(new DefaultValue<>(defaultValue)::getOrDefault);
    }

    public FlowBuilderBase<T> defaultValue(SerializableSupplier<T> defaultValue) {
        return map(new DefaultValueFromSupplier<>(defaultValue)::getOrDefault);
    }

    //PROCESSING - START
    public <R> FlowBuilderBase<R> map(SerializableFunction<T, R> mapFunction) {
        return super.mapBase(mapFunction);
    }

    public <S, R> FlowBuilderBase<R> mapBiFunction(SerializableBiFunction<T, S, R> int2IntFunction,
                                                   FlowBuilderBase<S> stream2Builder) {

        TriggeredFlowFunction<T> e1 = eventStream;
        return new FlowBuilderBase<>(
                new BinaryMapToRefFlowFunction<>(
                        eventStream, stream2Builder.eventStream, int2IntFunction)
        );
    }

    public FlowBuilderBase<T> merge(FlowBuilderBase<? extends T> streamToMerge) {
        return new FlowBuilderBase<>(new MergeFlowFunction<>(eventStream, streamToMerge.eventStream));
    }

//    public <S, R> EventStreamBuilderBase<R> flatMap(SerializableFunction<T, Iterable<R>> iterableFunction) {
//        return new EventStreamBuilderBase<>(new FlatMapEventStream<>(eventStream, iterableFunction));
//    }
//
//    public <S, R> EventStreamBuilderBase<R> flatMapFromArray(SerializableFunction<T, R[]> iterableFunction) {
//        return new EventStreamBuilderBase<>(new FlatMapArrayEventStream<>(eventStream, iterableFunction));
//    }

    public <S, R, F extends AggregateFlowFunction<T, R, F>> FlowBuilderBase<R>
    aggregate(SerializableSupplier<F> aggregateFunction) {
        return new FlowBuilderBase<>(new AggregateFlowFunctionWrapper<>(eventStream, aggregateFunction));
    }

    public <S, R, F extends AggregateFlowFunction<T, R, F>> FlowBuilderBase<R>
    tumblingAggregate(SerializableSupplier<F> aggregateFunction, int bucketSizeMillis) {
        return new FlowBuilderBase<>(
                new TumblingWindow<>(eventStream, aggregateFunction, bucketSizeMillis));
    }

    public <S, R, F extends AggregateFlowFunction<T, R, F>> FlowBuilderBase<R>
    slidingAggregate(SerializableSupplier<F> aggregateFunction, int bucketSizeMillis, int bucketsPerWindow) {
        return new FlowBuilderBase<>(
                new TimedSlidingWindow<>(eventStream, aggregateFunction, bucketSizeMillis, bucketsPerWindow));
    }

    public <V, K1, A, F extends AggregateFlowFunction<V, A, F>> GroupByFlowBuilder<K1, A>
    groupBy(SerializableFunction<T, K1> keyFunction,
            SerializableFunction<T, V> valueFunction,
            SerializableSupplier<F> aggregateFunctionSupplier) {
        MapFlowFunction<T, GroupBy<K1, A>, TriggeredFlowFunction<T>> x = new MapRef2RefFlowFunction<>(eventStream,
                new GroupByFlowFunctionWrapper<>(keyFunction, valueFunction, aggregateFunctionSupplier)::aggregate);
        return new GroupByFlowBuilder<>(x);
    }

    public <V, K1> GroupByFlowBuilder<K1, V>
    groupBy(SerializableFunction<T, K1> keyFunction,
            SerializableFunction<T, V> valueFunction) {
        return groupBy(keyFunction, valueFunction, Aggregates.identityFactory());
    }

    public <K> GroupByFlowBuilder<K, T>
    groupBy(SerializableFunction<T, K> keyFunction) {
        return groupBy(keyFunction, Mappers::identity);
    }

    public <V, K> GroupByFlowBuilder<K, List<T>>
    groupByAsList(SerializableFunction<T, K> keyFunction) {
        return groupBy(keyFunction, Mappers::identity, Collectors.listFactory());
    }

    public <V, K> GroupByFlowBuilder<K, List<T>>
    groupByAsList(SerializableFunction<T, K> keyFunction, int maxElementsInList) {
        return groupBy(keyFunction, Mappers::identity, Collectors.listFactory(maxElementsInList));
    }

    public <V, K, A, F extends AggregateFlowFunction<V, A, F>> GroupByFlowBuilder<K, A>
    groupByTumbling(SerializableFunction<T, K> keyFunction,
                    SerializableFunction<T, V> valueFunction,
                    SerializableSupplier<F> aggregateFunctionSupplier,
                    int bucketSizeMillis) {
        return new GroupByFlowBuilder<>(new GroupByTumblingWindow<>(
                eventStream,
                aggregateFunctionSupplier,
                keyFunction,
                valueFunction,
                bucketSizeMillis
        ));
    }

    public <V, K> GroupByFlowBuilder<K, V>
    groupByTumbling(SerializableFunction<T, K> keyFunction,
                    SerializableFunction<T, V> valueFunction,
                    int bucketSizeMillis) {
        return groupByTumbling(keyFunction, valueFunction, Aggregates.identityFactory(), bucketSizeMillis);
    }

    public <V, K, A, F extends AggregateFlowFunction<V, A, F>> GroupByFlowBuilder<K, A>
    groupBySliding(SerializableFunction<T, K> keyFunction,
                   SerializableFunction<T, V> valueFunction,
                   SerializableSupplier<F> aggregateFunctionSupplier,
                   int bucketSizeMillis,
                   int numberOfBuckets) {
        return new GroupByFlowBuilder<>(new GroupByTimedSlidingWindow<>(
                eventStream,
                aggregateFunctionSupplier,
                keyFunction,
                valueFunction,
                bucketSizeMillis,
                numberOfBuckets
        ));
    }

    public <V, K> GroupByFlowBuilder<K, V>
    groupBySliding(SerializableFunction<T, K> keyFunction,
                   SerializableFunction<T, V> valueFunction,
                   int bucketSizeMillis,
                   int numberOfBuckets) {
        return groupBySliding(keyFunction, valueFunction, Aggregates.identityFactory(), bucketSizeMillis, numberOfBuckets);
    }

    public <K, A, F extends AggregateFlowFunction<T, A, F>> GroupByFlowBuilder<K, A>
    groupBySliding(SerializableFunction<T, K> keyFunction,
                   SerializableSupplier<F> aggregateFunctionSupplier,
                   int bucketSizeMillis,
                   int numberOfBuckets) {
        return new GroupByFlowBuilder<>(new GroupByTimedSlidingWindow<>(
                eventStream,
                aggregateFunctionSupplier,
                keyFunction,
                Mappers::identity,
                bucketSizeMillis,
                numberOfBuckets
        ));
    }

    public <I, Z extends FlowBuilderBase<I>> Z mapOnNotify(I target) {
        return super.mapOnNotifyBase(target);
    }


}
