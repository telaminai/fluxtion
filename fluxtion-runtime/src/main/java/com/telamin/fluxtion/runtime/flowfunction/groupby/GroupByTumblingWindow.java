/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.annotations.NoTriggerReference;
import com.telamin.fluxtion.runtime.annotations.OnParentUpdate;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.annotations.builder.SepNode;
import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.TriggeredFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.AbstractFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;
import com.telamin.fluxtion.runtime.time.FixedRateTrigger;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * @param <T> Input type
 * @param <R> Output of aggregate function
 * @param <K> Key type from input T
 * @param <V> Value type from input T, input to aggregate function
 * @param <S> {@link FlowFunction} input type
 * @param <F>
 */
public class GroupByTumblingWindow<T, K, V, R, S extends FlowFunction<T>, F extends AggregateFlowFunction<V, R, F>>
        extends AbstractFlowFunction<T, GroupBy<K, R>, S>
        implements TriggeredFlowFunction<GroupBy<K, R>> {

    @SepNode
    @NoTriggerReference
    public GroupByFlowFunctionWrapper<T, K, V, R, F> groupByWindowedCollection;
    public FixedRateTrigger rollTrigger;

    // LinkedHashMap: preserve first-key-seen order so a multi-key window roll emits deterministically (see
    // GroupByHashMap). Fed by putAll(groupByWindowedCollection.toMap()), which is itself insertion-ordered.
    private transient final Map<K, R> mapOfValues = new LinkedHashMap<>();
    private transient final MyGroupBy results = new MyGroupBy();

    public GroupByTumblingWindow(
            S inputEventStream,
            SerializableSupplier<F> windowFunctionSupplier,
            SerializableFunction<T, K> keyFunction,
            SerializableFunction<T, V> valueFunction,
            int windowSizeMillis) {
        this(inputEventStream);
        this.groupByWindowedCollection = new GroupByFlowFunctionWrapper<>(keyFunction, valueFunction, windowFunctionSupplier);
        rollTrigger = FixedRateTrigger.atMillis(windowSizeMillis);
    }

    public GroupByTumblingWindow(S inputEventStream) {
        super(inputEventStream, null);
    }

    @Override
    public GroupBy<K, R> get() {
        return results;
    }

    protected void cacheWindowValue() {
        mapOfValues.clear();
        mapOfValues.putAll(groupByWindowedCollection.toMap());
    }

    protected void aggregateInputValue(S inputEventStream) {
        groupByWindowedCollection.aggregate(inputEventStream.get());
    }

    @OnParentUpdate
    public void timeTriggerFired(FixedRateTrigger rollTrigger) {
        if (rollTrigger.getTriggerCount() == 1) {
            cacheWindowValue();
        }
        publishOverrideTriggered = !overridePublishTrigger & !overrideUpdateTrigger;
        inputStreamTriggered_1 = true;
        inputStreamTriggered = true;
        groupByWindowedCollection.reset();
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

    @Override
    protected void resetOperation() {
        mapOfValues.clear();
        groupByWindowedCollection.reset();
    }

    @Override
    public boolean isStatefulFunction() {
        return true;
    }

    @OnTrigger
    public boolean triggered() {
        return fireEventUpdateNotification();
    }


    private class MyGroupBy implements GroupBy<K, R> {

        @Override
        public Map<K, R> toMap() {
            return mapOfValues;
        }

        @Override
        public Collection<R> values() {
            return mapOfValues.values();
        }

        @Override
        public R lastValue() {
            return groupByWindowedCollection.lastValue();
        }

        @Override
        public KeyValue<K, R> lastKeyValue() {
            return groupByWindowedCollection.lastKeyValue();
        }
    }
}
