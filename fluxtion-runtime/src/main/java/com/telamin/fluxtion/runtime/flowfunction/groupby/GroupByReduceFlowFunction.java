/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Delta-aware groupBy reduce (P2c of the GroupBy delta IVM scope,
 * {@code docs/design/groupby-delta-ivm.md}).
 *
 * <p>Collapses all values of a group into one scalar via an {@link AggregateFlowFunction}. Unlike the
 * other operators the output is a <em>scalar</em>, not a {@link GroupBy} — so there is no output delta;
 * the optimisation is on the <em>input</em> side: maintain the running aggregate in O(Δ) from the input
 * {@link GroupByDelta} instead of re-aggregating every value each cycle.
 *
 * <p><b>Gated on invertibility</b> (same algebra as the sliding-window deduct fix). An aggregate can be
 * maintained incrementally only when it is a group — {@link AggregateFlowFunction#deductSupported()}
 * true (sum/count/average): a per-key {@code UPDATE}/{@code DELETE} deducts the old value
 * (via a scratch aggregate) and adds the new. A semilattice reducer (min/max,
 * {@code deductSupported()} false) has no inverse, so it always recomputes — never silently wrong.
 * {@link DeltaMode#RECOMPUTE_REQUIRED} / {@link DeltaMode#CLEAR_THEN_APPLY} upstream also recompute
 * (the bulk paths), rebuilding the {@code seen} snapshot the incremental path deducts against.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class GroupByReduceFlowFunction {

    private final SerializableSupplier aggregateFunctionSupplier;
    private final transient AggregateFlowFunction aggregateFunction;
    private final transient AggregateFlowFunction deductScratch;
    private final transient boolean deductSupported;
    private final transient Map<Object, Object> seen = new HashMap<>();

    public GroupByReduceFlowFunction(
            @AssignToField("aggregateFunctionSupplier")
            SerializableSupplier aggregateFunctionSupplier) {
        this.aggregateFunctionSupplier = aggregateFunctionSupplier;
        this.aggregateFunction = (AggregateFlowFunction) aggregateFunctionSupplier.get();
        this.deductScratch = (AggregateFlowFunction) aggregateFunctionSupplier.get();
        this.deductSupported = aggregateFunction.deductSupported();
    }

    public <R> R reduceValues(GroupBy inputMap) {
        GroupByDelta delta = inputMap.delta();
        if (!deductSupported
                || delta.mode() == DeltaMode.RECOMPUTE_REQUIRED
                || delta.mode() == DeltaMode.CLEAR_THEN_APPLY) {
            return (R) recompute(inputMap);
        }

        // INCREMENTAL + invertible: update the running aggregate by Δ only.
        List<Change> entries = delta.entries();
        for (int i = 0; i < entries.size(); i++) {
            Change c = entries.get(i);
            switch (c.op()) {
                case ADD:
                    aggregateFunction.aggregate(c.value());
                    seen.put(c.key(), c.value());
                    break;
                case UPDATE:
                    deductValue(seen.get(c.key()));
                    aggregateFunction.aggregate(c.value());
                    seen.put(c.key(), c.value());
                    break;
                case DELETE:
                    deductValue(seen.remove(c.key()));
                    break;
                default:
                    throw new IllegalStateException("unknown ChangeOp " + c.op());
            }
        }
        return (R) aggregateFunction.get();
    }

    private Object recompute(GroupBy inputMap) {
        aggregateFunction.reset();
        seen.clear();
        Map<Object, Object> in = inputMap.toMap();
        for (Map.Entry<Object, Object> e : in.entrySet()) {
            aggregateFunction.aggregate(e.getValue());
            seen.put(e.getKey(), e.getValue());
        }
        return aggregateFunction.get();
    }

    /** Subtract a single value's contribution via a scratch aggregate (the per-value inverse). */
    private void deductValue(Object value) {
        deductScratch.reset();
        deductScratch.aggregate(value);
        aggregateFunction.deduct(deductScratch);
    }

    //required for serialised version
    public Object reduceValues(Object inputMap) {
        return reduceValues((GroupBy) inputMap);
    }
}
