/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.flowfunction;

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.runtime.context.buildtime.GeneratorNodeCollection;
import com.telamin.fluxtion.runtime.flowfunction.TriggeredFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.Tuple;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.BinaryMapFlowFunction.BinaryMapToRefFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.MapFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.MapFlowFunction.MapRef2RefFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupByDeleteByKeyFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.helpers.DefaultValue;
import com.telamin.fluxtion.runtime.flowfunction.helpers.DefaultValue.DefaultValueFromSupplier;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Peekers;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Tuples;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableBiFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class GroupByFlowBuilder<K, V> extends AbstractGroupByBuilder<K, V, com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy<K, V>> {

    public GroupByFlowBuilder(TriggeredFlowFunction<com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy<K, V>> eventStream) {
        super(eventStream);
    }

    public <I, G extends com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy<K, V>> GroupByFlowBuilder(MapFlowFunction<I, com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy<K, V>, TriggeredFlowFunction<I>> eventStream) {
        super(eventStream);
    }

    @Override
    protected GroupByFlowBuilder<K, V> identity() {
        return this;
    }

    //
    public GroupByFlowBuilder<K, V> updateTrigger(Object updateTrigger) {
        eventStream.setUpdateTriggerNode(StreamHelper.getSource(updateTrigger));
        return identity();
    }

    public GroupByFlowBuilder<K, V> updateTrigger(Object... publishTrigger) {
        eventStream.setUpdateTriggerNode(PredicateBuilder.anyTriggered(publishTrigger));
        return identity();
    }

    public GroupByFlowBuilder<K, V> publishTrigger(Object publishTrigger) {
        eventStream.setPublishTriggerNode(StreamHelper.getSource(publishTrigger));
        return identity();
    }

    public GroupByFlowBuilder<K, V> publishTrigger(Object... publishTrigger) {
        eventStream.setPublishTriggerNode(PredicateBuilder.anyTriggered(publishTrigger));
        return identity();
    }

    public GroupByFlowBuilder<K, V> publishTriggerOverride(Object publishTrigger) {
        eventStream.setPublishTriggerOverrideNode(StreamHelper.getSource(publishTrigger));
        return identity();
    }

    public GroupByFlowBuilder<K, V> publishTriggerOverride(Object... publishTrigger) {
        eventStream.setPublishTriggerOverrideNode(PredicateBuilder.anyTriggered(publishTrigger));
        return identity();
    }

    public GroupByFlowBuilder<K, V> resetTrigger(Object resetTrigger) {
        eventStream.setResetTriggerNode(StreamHelper.getSource(resetTrigger));
        return identity();
    }

    public GroupByFlowBuilder<K, V> resetTrigger(Object... publishTrigger) {
        eventStream.setResetTriggerNode(PredicateBuilder.anyTriggered(publishTrigger));
        return identity();
    }

    public GroupByFlowBuilder<K, V> defaultValue(com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy<K, V> defaultValue) {
        return new GroupByFlowBuilder<>(new MapRef2RefFlowFunction<>(eventStream,
                new DefaultValue<>(defaultValue)::getOrDefault));
    }

    public GroupByFlowBuilder<K, V> defaultValue(SerializableSupplier<com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy<K, V>> defaultValue) {
        return new GroupByFlowBuilder<>(new MapRef2RefFlowFunction<>(eventStream,
                new DefaultValueFromSupplier<>(defaultValue)::getOrDefault));
    }

    public <O> GroupByFlowBuilder<K, O> mapValues(SerializableFunction<V, O> mappingFunction) {
        return new GroupByFlowBuilder<>(new MapRef2RefFlowFunction<>(eventStream,
                new com.telamin.fluxtion.runtime.flowfunction.groupby.GroupByMapFlowFunction(mappingFunction)::mapValues));
    }

    public <R, F extends AggregateFlowFunction<V, R, F>> FlowBuilder<R> reduceValues(
            SerializableSupplier<F> aggregateFactory) {
        return new FlowBuilder<>(new MapRef2RefFlowFunction<>(eventStream,
                new com.telamin.fluxtion.runtime.flowfunction.groupby.GroupByReduceFlowFunction(aggregateFactory.get())::reduceValues));
    }


    public <O> GroupByFlowBuilder<O, V> mapKeys(SerializableFunction<K, O> mappingFunction) {
        return new GroupByFlowBuilder<>(new MapRef2RefFlowFunction<>(eventStream,
                new com.telamin.fluxtion.runtime.flowfunction.groupby.GroupByMapFlowFunction(mappingFunction)::mapKeys));
    }

    public <K1, V1, G extends Map.Entry<K, V>> GroupByFlowBuilder<K1, V1> mapEntries(
            SerializableFunction<G, Map.Entry<K1, V1>> mappingFunction) {
        return new GroupByFlowBuilder<>(new MapRef2RefFlowFunction<>(eventStream,
                new com.telamin.fluxtion.runtime.flowfunction.groupby.GroupByMapFlowFunction(mappingFunction)::mapEntry));
    }

    /**
     * @param supplierOfIdsToDelete a data flow of id's to delete
     * @return The GroupByFlowBuilder with delete function applied
     * @see #deleteByKey(FlowBuilder, boolean)
     */
    public GroupByFlowBuilder<K, V> deleteByKey(SerializableSupplier<Collection<K>> supplierOfIdsToDelete) {
        return deleteByKey(DataFlowBuilder.subscribeToNodeProperty(supplierOfIdsToDelete), false);
    }

    /**
     * @param supplierOfIdsToDelete       a data flow of id's to delete
     * @param clearDeleteIdsAfterApplying flag to clear the delete id's after applying
     * @return The GroupByFlowBuilder with delete function applied
     * @see #deleteByKey(FlowBuilder, boolean)
     */
    public GroupByFlowBuilder<K, V> deleteByKey(SerializableSupplier<Collection<K>> supplierOfIdsToDelete, boolean clearDeleteIdsAfterApplying) {
        return deleteByKey(DataFlowBuilder.subscribeToNodeProperty(supplierOfIdsToDelete), clearDeleteIdsAfterApplying);
    }

    /**
     * @param supplierOfIdsToDelete a data flow of id's to delete
     * @return The GroupByFlowBuilder with delete function applied
     * @see #deleteByKey(FlowBuilder, boolean)
     */
    public GroupByFlowBuilder<K, V> deleteByKey(FlowBuilder<Collection<K>> supplierOfIdsToDelete) {
        return deleteByKey(supplierOfIdsToDelete, false);
    }

    /**
     * Deletes items from a {@link com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy} collection by their id's. A supplier of keys to delete is applied to the
     * GroupBy. The collection of id's can be cleared after applying or remain in place with the clearDeleteIdsAfterApplying
     *
     * @param supplierOfIdsToDelete       a data flow of id's to delete
     * @param clearDeleteIdsAfterApplying flag to clear the delete id's after applying
     * @return The GroupByFlowBuilder with delete function applied
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public GroupByFlowBuilder<K, V> deleteByKey(FlowBuilder<Collection<K>> supplierOfIdsToDelete, boolean clearDeleteIdsAfterApplying) {
        return new GroupByFlowBuilder<>(
                new BinaryMapToRefFlowFunction<>(
                        eventStream,
                        supplierOfIdsToDelete.defaultValue(Collections::emptyList).eventStream,
                        new GroupByDeleteByKeyFlowFunction(supplierOfIdsToDelete.flowSupplier(), clearDeleteIdsAfterApplying)::deleteByKey))
                .defaultValue(new com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy.EmptyGroupBy<>());
    }

    /**
     * Deletes items from a {@link com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy} collection using a predicate function applied to an elements value.
     *
     * @param deletePredicateFunction the predicate function that determines if an element should  be deleted. Deletes if returns true
     * @return The GroupByFlowBuilder with delete function applied
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public GroupByFlowBuilder<K, V> deleteByValue(SerializableFunction<V, Boolean> deletePredicateFunction) {
        Object functionInstance = deletePredicateFunction.captured()[0];
        FlowBuilder<Object> deleteTestFlow = DataFlowBuilder.subscribeToNode(functionInstance);
        return new GroupByFlowBuilder<>(
                new BinaryMapToRefFlowFunction<>(
                        eventStream,
                        deleteTestFlow.defaultValue(functionInstance).eventStream,
                        new com.telamin.fluxtion.runtime.flowfunction.groupby.GroupByDeleteByNameFlowFunctionWrapper(deletePredicateFunction, functionInstance)::deleteByKey))
                .defaultValue(new com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy.EmptyGroupBy<>());
    }

    public GroupByFlowBuilder<K, V> filterValues(SerializableFunction<V, Boolean> mappingFunction) {
        return new GroupByFlowBuilder<>(new MapRef2RefFlowFunction<>(eventStream,
                new com.telamin.fluxtion.runtime.flowfunction.groupby.GroupByFilterFlowFunctionWrapper(mappingFunction)::filterValues));
    }

    public <K2 extends K, V2> GroupByFlowBuilder<K, Tuple<V, V2>> innerJoin(GroupByFlowBuilder<K2, V2> rightGroupBy) {
        return mapBiFunction(new com.telamin.fluxtion.runtime.flowfunction.groupby.InnerJoin()::join, rightGroupBy);
    }

    public <K2 extends K, V2, R> GroupByFlowBuilder<K, R> innerJoin(
            GroupByFlowBuilder<K2, V2> rightGroupBy,
            SerializableBiFunction<V, V2, R> mergeFunction) {
        return mapBiFunction(new com.telamin.fluxtion.runtime.flowfunction.groupby.InnerJoin()::join, rightGroupBy).mapValues(Tuples.mapTuple(mergeFunction));
    }

    public <K2 extends K, V2> GroupByFlowBuilder<K, Tuple<V, V2>> outerJoin(GroupByFlowBuilder<K2, V2> rightGroupBy) {
        return mapBiFunction(new com.telamin.fluxtion.runtime.flowfunction.groupby.OuterJoin()::join, rightGroupBy);
    }

    public <K2 extends K, V2, R> GroupByFlowBuilder<K, R> outerJoin(
            GroupByFlowBuilder<K2, V2> rightGroupBy,
            SerializableBiFunction<V, V2, R> mergeFunction) {
        return mapBiFunction(new com.telamin.fluxtion.runtime.flowfunction.groupby.OuterJoin()::join, rightGroupBy).mapValues(Tuples.mapTuple(mergeFunction));
    }

    public <K2 extends K, V2> GroupByFlowBuilder<K, Tuple<V, V2>> leftJoin(GroupByFlowBuilder<K2, V2> rightGroupBy) {
        return mapBiFunction(new com.telamin.fluxtion.runtime.flowfunction.groupby.LeftJoin()::join, rightGroupBy);
    }

    public <K2 extends K, V2, R> GroupByFlowBuilder<K, R> leftJoin(
            GroupByFlowBuilder<K2, V2> rightGroupBy,
            SerializableBiFunction<V, V2, R> mergeFunction) {
        return mapBiFunction(new com.telamin.fluxtion.runtime.flowfunction.groupby.LeftJoin()::join, rightGroupBy).mapValues(Tuples.mapTuple(mergeFunction));
    }

    public <K2 extends K, V2> GroupByFlowBuilder<K, Tuple<V, V2>> rightJoin(GroupByFlowBuilder<K2, V2> rightGroupBy) {
        return mapBiFunction(new com.telamin.fluxtion.runtime.flowfunction.groupby.RightJoin()::join, rightGroupBy);
    }

    public <K2 extends K, V2, R> GroupByFlowBuilder<K, R> rightJoin(
            GroupByFlowBuilder<K2, V2> rightGroupBy,
            SerializableBiFunction<V, V2, R> mergeFunction) {
        return mapBiFunction(new com.telamin.fluxtion.runtime.flowfunction.groupby.RightJoin()::join, rightGroupBy).mapValues(Tuples.mapTuple(mergeFunction));
    }

    public <K2, V2, KOUT, VOUT>
    GroupByFlowBuilder<KOUT, VOUT> mapBiFunction(
            SerializableBiFunction<com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy<K, V>, com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy<K2, V2>, com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy<KOUT, VOUT>> int2IntFunction,
            GroupByFlowBuilder<K2, V2> stream2Builder) {
        return new GroupByFlowBuilder<>(
                new BinaryMapToRefFlowFunction<>(eventStream, stream2Builder.eventStream, int2IntFunction)
                        .defaultValue(new com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy.EmptyGroupBy<>())
        );
    }

    public <K2, V2, KOUT, VOUT> GroupByFlowBuilder<KOUT, VOUT> mapBi(
            GroupByFlowBuilder<K2, V2> stream2Builder,
            SerializableBiFunction<com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy<K, V>, com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy<K2, V2>, com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy<KOUT, VOUT>> int2IntFunction) {
        return mapBiFunction(int2IntFunction, stream2Builder);
    }

    public <V2, KOUT, VOUT>
    GroupByFlowBuilder<KOUT, VOUT> mapBiFlowFunction(
            SerializableBiFunction<com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy<K, V>, V2, com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy<KOUT, VOUT>> int2IntFunction,
            FlowBuilder<V2> stream2Builder) {
        return new GroupByFlowBuilder<>(
                new BinaryMapToRefFlowFunction<>(eventStream, stream2Builder.eventStream, int2IntFunction)
                        .defaultValue(new com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy.EmptyGroupBy<>())
        );
    }

    public <V2, KOUT, VOUT>
    GroupByFlowBuilder<KOUT, VOUT> mapBiFlow(
            FlowBuilder<V2> stream2Builder,
            SerializableBiFunction<com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy<K, V>, V2, com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy<KOUT, VOUT>> int2IntFunction) {
        return new GroupByFlowBuilder<>(
                new BinaryMapToRefFlowFunction<>(eventStream, stream2Builder.eventStream, int2IntFunction)
                        .defaultValue(new com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy.EmptyGroupBy<>())
        );
    }

    public GroupByFlowBuilder<K, V> console(String in) {
        peek(Peekers.console(in, null));
        return identity();
    }

    public GroupByFlowBuilder<K, V> console() {
        return console("{}");
    }

    //META-DATA
    public GroupByFlowBuilder<K, V> id(String nodeId) {
        GeneratorNodeCollection.service().add(eventStream, nodeId);
        return identity();
    }
}
