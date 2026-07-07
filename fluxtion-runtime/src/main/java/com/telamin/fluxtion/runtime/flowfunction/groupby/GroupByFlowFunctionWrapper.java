/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.flowfunction.Stateful;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @param <T> Input type
 * @param <K> Key type from input T
 * @param <V> Value type from input T, input to aggregate function
 * @param <A> output type of aggregate calculation
 * @param <F> The aggregate function converts a V into an A
 */
public class GroupByFlowFunctionWrapper<T, K, V, A, F extends AggregateFlowFunction<V, A, F>>
        implements AggregateFlowFunction<T, GroupBy<K, A>, GroupByFlowFunctionWrapper<T, K, V, A, F>>,
        GroupBy<K, A>, Stateful<GroupBy<K, A>> {

    private final SerializableFunction<T, K> keyFunction;
    private final SerializableFunction<T, V> valueFunction;
    private final SerializableSupplier<F> aggregateFunctionSupplier;
    private transient final Map<K, F> mapOfFunctions;
    private transient final Map<K, A> mapOfValues;
    private transient final Map<K, AtomicLong> keyCount;
    private F latestAggregateValue;
    private KeyValue<K, A> keyValue;
    private transient GroupByDelta<K, A> delta = GroupByDelta.recomputeRequired();
    // P3: combine/deduct touch many keys across one window roll; accumulate the net per-key change set
    // and expose it as a single multi-key delta. The roll's calls (combine then deduct, possibly
    // repeated for catch-up) accumulate into one cycle; reading delta() marks the cycle consumed so the
    // next roll's first mutation starts fresh.
    private transient final Map<K, Change<K, A>> deltaAccumulator = new HashMap<>();
    private transient boolean deltaConsumed = true;
    private transient boolean accumulatorCleared = false;

    public GroupByFlowFunctionWrapper(
            @AssignToField("keyFunction")
            SerializableFunction<T, K> keyFunction,
            @AssignToField("valueFunction")
            SerializableFunction<T, V> valueFunction,
            @AssignToField("aggregateFunctionSupplier")
            SerializableSupplier<F> aggregateFunctionSupplier) {
        this.keyFunction = keyFunction;
        this.valueFunction = valueFunction;
        this.aggregateFunctionSupplier = aggregateFunctionSupplier;
        this.mapOfFunctions = new HashMap<>();
        // LinkedHashMap: mapOfValues is the root aggregation store whose toMap() feeds every window's output and
        // the join recompute path — first-key-seen order here makes multi-key emit deterministic and identical
        // interpreted/AOT (see GroupByHashMap). mapOfFunctions/keyCount are bookkeeping, never iterated for emit.
        this.mapOfValues = new LinkedHashMap<>();
        this.keyCount = new HashMap<>();
    }

    @Override
    public GroupBy<K, A> get() {
        return this;
    }

    /**
     * Delegate to the inner per-key aggregate. The wrapper's {@code combine}/{@code deduct} are only
     * invertible when the inner aggregate is — e.g. sum/count/average. For a non-invertible inner
     * aggregate (min/max), this returns {@code false} so {@code BucketedSlidingWindow} takes the
     * full-recompute branch instead of calling the inner {@code deduct} (which throws).
     */
    @Override
    public boolean deductSupported() {
        return aggregateFunctionSupplier.get().deductSupported();
    }

    @Override
    public void combine(GroupByFlowFunctionWrapper<T, K, V, A, F> add) {
        beginCycleIfConsumed();
        //merge each if existing
        add.mapOfFunctions.forEach((k, f) -> {
            boolean newKey = !mapOfFunctions.containsKey(k);
            F targetFunction = mapOfFunctions.computeIfAbsent(k, key -> aggregateFunctionSupplier.get());
            keyCount.computeIfAbsent(k, key -> new AtomicLong()).incrementAndGet();
            targetFunction.combine(f);
            A value = targetFunction.get();
            mapOfValues.put(k, value);
            // P3: record the per-key change; ADD/UPDATE precision is informational (downstream operators
            // derive membership from their own state), the value is what must be exact.
            deltaAccumulator.put(k, new Change<>(k, value, newKey ? ChangeOp.ADD : ChangeOp.UPDATE));
        });
        delta = null; // lazily rebuilt from the accumulator on read
    }

    @Override
    public void deduct(GroupByFlowFunctionWrapper<T, K, V, A, F> add) {
        beginCycleIfConsumed();
        //ignore if
        add.mapOfFunctions.forEach((k, f) -> {
            AtomicLong currentCount = keyCount.computeIfAbsent(k, key -> new AtomicLong());
            currentCount.decrementAndGet();
            if (currentCount.intValue() < 1) {
                currentCount.set(0);
                //remove completely
                mapOfFunctions.remove(k);
                A previous = mapOfValues.remove(k);
                deltaAccumulator.put(k, Change.delete(k, previous));
            } else {
                //perform deduct
                F targetFunction = mapOfFunctions.get(k);
                targetFunction.deduct(f);
                A value = targetFunction.get();
                mapOfValues.put(k, value);
                deltaAccumulator.put(k, new Change<>(k, value, ChangeOp.UPDATE));
            }
        });
        delta = null; // lazily rebuilt from the accumulator on read
    }

    /**
     * Start a fresh accumulation cycle once the previous delta has been read (consumed). A window roll
     * issues several {@code combine}/{@code deduct} calls that must accumulate into ONE delta; the first
     * mutation after a read clears, subsequent mutations in the same roll append.
     */
    private void beginCycleIfConsumed() {
        if (deltaConsumed) {
            deltaAccumulator.clear();
            accumulatorCleared = false;
            deltaConsumed = false;
        }
    }

    public GroupBy<K, A> aggregate(T input) {
        K key = keyFunction.apply(input);
        V value = valueFunction.apply(input);
        F currentFunction = mapOfFunctions.get(key);
        boolean newKey = currentFunction == null;
        if (newKey) {
            currentFunction = aggregateFunctionSupplier.get();
            mapOfFunctions.put(key, currentFunction);
            keyCount.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
        }
        currentFunction.aggregate(value);
        latestAggregateValue = currentFunction;
        A aggregated = latestAggregateValue.get();
        mapOfValues.put(key, aggregated);
        keyValue = new KeyValue<>(key, aggregated);
        // P1 single-key producer delta: one event changes exactly one key.
        delta = GroupByDelta.incremental(Collections.<Change<K, A>>singletonList(
                new Change<>(key, aggregated, newKey ? ChangeOp.ADD : ChangeOp.UPDATE)));
        return this;
    }

    @Override
    public GroupByDelta<K, A> delta() {
        if (delta == null) {
            // P3 window path: build the net multi-key delta from the accumulator. A reset() that began
            // this cycle makes it CLEAR_THEN_APPLY (the non-invertible roll's clear-then-rebuild and the
            // tumbling boundary); otherwise it is an incremental slide (combine + deduct).
            List<Change<K, A>> entries = new ArrayList<>(deltaAccumulator.values());
            delta = accumulatorCleared
                    ? GroupByDelta.clearThenApply(entries)
                    : GroupByDelta.incremental(entries);
        }
        deltaConsumed = true;
        return delta;
    }

    @Override
    public KeyValue<K, A> lastKeyValue() {
        return keyValue;
    }

    @Override
    public Map<K, A> toMap() {
        return mapOfValues;
    }

    @Override
    public A lastValue() {
        return latestAggregateValue.get();
    }

    @Override
    public Collection<A> values() {
        return toMap().values();
    }

    @Override
    public GroupBy<K, A> reset() {
        mapOfFunctions.clear();
        mapOfValues.clear();
        keyCount.clear();   // the recompute branch resets + re-combines every roll; combine increments
                            // keyCount, so without this clear the map grows unbounded (a slow leak).
        keyValue = null;
        // P3: begin a CLEAR_THEN_APPLY cycle. The non-invertible sliding roll does reset() then
        // combine() per live bucket, and the tumbling boundary resets between windows; either way the
        // accumulated entries become the post-clear replacement set. With no following combine this is
        // an empty CLEAR_THEN_APPLY (== the old cleared()).
        deltaAccumulator.clear();
        accumulatorCleared = true;
        deltaConsumed = false;
        delta = null;
        return this;
    }

    @Override
    public String toString() {
        return "GroupByFlowFunctionWrapper{" +
                "mapOfValues=" + mapOfValues +
                '}';
    }
}
