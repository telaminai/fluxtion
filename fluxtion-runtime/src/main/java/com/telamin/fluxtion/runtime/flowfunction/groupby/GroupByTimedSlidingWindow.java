/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.annotations.OnParentUpdate;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.TriggeredFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.BucketedSlidingWindow;
import com.telamin.fluxtion.runtime.flowfunction.function.AbstractFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;
import com.telamin.fluxtion.runtime.time.FixedRateTrigger;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;


/**
 * @param <T> Input type
 * @param <R> Output of aggregate function
 * @param <K> Key type from input T
 * @param <V> Value type from input T, input to aggregate function
 * @param <S> {@link FlowFunction} input type
 * @param <F>
 */
public class GroupByTimedSlidingWindow<T, K, V, R, S extends FlowFunction<T>, F extends AggregateFlowFunction<V, R, F>>
        extends AbstractFlowFunction<T, GroupBy<K, R>, S>
        implements TriggeredFlowFunction<GroupBy<K, R>> {

    private final SerializableSupplier<F> windowFunctionSupplier;
    private final SerializableFunction<T, K> keyFunction;
    private final SerializableFunction<T, V> valueFunction;
    private final int bucketSizeMillis;
    private final int bucketCount;
    public FixedRateTrigger rollTrigger;
    private transient Supplier<GroupByFlowFunctionWrapper<T, K, V, R, F>> groupBySupplier;
    private transient BucketedSlidingWindow<T, GroupBy<K, R>, GroupByFlowFunctionWrapper<T, K, V, R, F>> slidingCalculator;
    // LinkedHashMap: first-key-seen order → deterministic multi-key emit, identical interpreted/AOT (see
    // GroupByHashMap / GroupByTumblingWindow).
    private transient final Map<K, R> mapOfValues = new LinkedHashMap<>();
    private transient final MyGroupBy results = new MyGroupBy();
    // P3 layer 2 (sliding-invertible only). Bootstrap-safe defaults: false/null so AOT transient zeroing
    // can never accidentally treat a later publish as the first one.
    private transient boolean windowBootstrapped = false;
    private transient boolean publishIsBootstrap = false;
    private transient Boolean deductSupportedCache;


    public GroupByTimedSlidingWindow(
            S inputEventStream,
            SerializableSupplier<F> windowFunctionSupplier,
            @AssignToField("keyFunction")
            SerializableFunction<T, K> keyFunction,
            @AssignToField("valueFunction")
            SerializableFunction<T, V> valueFunction,
            @AssignToField("bucketSizeMillis")
            int bucketSizeMillis,
            @AssignToField("bucketCount")
            int bucketCount) {
        super(inputEventStream, null);
        this.windowFunctionSupplier = windowFunctionSupplier;
        this.keyFunction = keyFunction;
        this.valueFunction = valueFunction;
        this.bucketSizeMillis = bucketSizeMillis;
        this.bucketCount = bucketCount;
        resetTriggered = false;
        rollTrigger = FixedRateTrigger.atMillis(bucketSizeMillis);
        groupBySupplier = () -> new GroupByFlowFunctionWrapper<>(keyFunction, valueFunction, windowFunctionSupplier);
        slidingCalculator = new BucketedSlidingWindow<>(groupBySupplier, bucketCount);
    }

    @Override
    public GroupBy<K, R> get() {
        return results;
    }

    protected void cacheWindowValue() {
        GroupBy<K, R> value = slidingCalculator.get();
        mapOfValues.clear();
        mapOfValues.putAll(value.toMap());
    }

    protected void aggregateInputValue(S inputEventStream) {
        slidingCalculator.aggregate(inputEventStream.get());
    }

    @OnParentUpdate
    public void timeTriggerFired(FixedRateTrigger rollTrigger) {
        slidingCalculator.roll(rollTrigger.getTriggerCount());
        if (slidingCalculator.isAllBucketsFilled()) {
            cacheWindowValue();
            // The first published window covers the whole warm-up the downstream never saw -> publish
            // RECOMPUTE_REQUIRED for it; every slide after is an O(Δ) incremental delta.
            publishIsBootstrap = !windowBootstrapped;
            windowBootstrapped = true;
            publishOverrideTriggered = !overridePublishTrigger & !overrideUpdateTrigger;
            inputStreamTriggered_1 = true;
            inputStreamTriggered = true;
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
        groupBySupplier = () -> new GroupByFlowFunctionWrapper<>(keyFunction, valueFunction, windowFunctionSupplier);
        slidingCalculator = new BucketedSlidingWindow<>(groupBySupplier, bucketCount);
        rollTrigger.init();
        mapOfValues.clear();
        windowBootstrapped = false; // a reset re-bootstraps: next first publish is RECOMPUTE_REQUIRED again
    }

    /** Whether the per-value aggregate is invertible — only then is a sliding delta exact. */
    private boolean deductSupported() {
        if (deductSupportedCache == null) {
            deductSupportedCache = windowFunctionSupplier.get().deductSupported();
        }
        return deductSupportedCache;
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
        public GroupByDelta<K, R> delta() {
            // Always consume the producer's accumulated roll delta to close its cycle (so the next roll
            // starts fresh), even when we then publish RECOMPUTE_REQUIRED.
            GroupByDelta<K, R> slide = slidingCalculator.get().delta();
            if (!deductSupported() || publishIsBootstrap) {
                return GroupByDelta.recomputeRequired();
            }
            return slide;
        }

        @Override
        public Collection<R> values() {
            return mapOfValues.values();
        }

        @Override
        public R lastValue() {
            return slidingCalculator.get().lastValue();
        }

        @Override
        public KeyValue<K, R> lastKeyValue() {
            return slidingCalculator.get().lastKeyValue();
        }
    }
}
