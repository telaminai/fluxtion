/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.annotations.NoTriggerReference;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Delta-aware {@code filterValues} (P2a of the GroupBy delta IVM scope,
 * {@code docs/design/groupby-delta-ivm.md}).
 *
 * <p>Maintains the filtered view <em>incrementally</em>: each cycle it reads the upstream
 * {@link GroupBy#delta()} and applies only the changed keys through the predicate, then publishes an
 * <em>output</em> {@link GroupByDelta} describing only the keys whose membership or value changed —
 * O(Δ), not O(n). The output {@code toMap()} is always the full filtered state, so map-derived
 * consumers are unaffected. The membership transitions (the IVM bug surface) are:
 *
 * <pre>
 *   input ADD    + passes              -> output ADD
 *   input ADD    + fails               -> (none)
 *   input UPDATE + was out -> now in   -> output ADD
 *   input UPDATE + was in  -> still in -> output UPDATE
 *   input UPDATE + was in  -> now out  -> output DELETE   (the classic bug row)
 *   input UPDATE + was out -> still out-> (none)
 *   input DELETE + was in              -> output DELETE
 *   input DELETE + was out             -> (none)
 *   CLEAR_THEN_APPLY                   -> clear, then ADD each passing replacement entry
 * </pre>
 *
 * <p>When the upstream offers no per-key changes ({@link DeltaMode#RECOMPUTE_REQUIRED} — e.g. a
 * windowed combine, P3) it rescans and over-approximates downstream as {@code RECOMPUTE_REQUIRED};
 * never silently drops a change.
 *
 * <p><b>Lowering.</b> {@link #filterValues(GroupBy)} is a single <em>non-generic</em> method: the
 * generator emits it as a bare instance method reference ({@code wrapper::filterValues}) into raw
 * codegen, and a generic {@code <K,V>} method there can't be re-emitted cleanly (the same reason the
 * {@link GroupByDeltaFlowFunctions} flat-map helpers are raw). The builder re-attaches the
 * {@code <K,V>} façade. This replaces the earlier {@code filterValues(Object)} bridge overload.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class GroupByFilterFlowFunctionWrapper {

    @NoTriggerReference
    private final SerializableFunction mapFunction;
    private final transient GroupByHashMap outputCollection = new GroupByHashMap();

    public <T> GroupByFilterFlowFunctionWrapper(SerializableFunction<T, Boolean> mapFunction) {
        this.mapFunction = mapFunction;
    }

    public GroupBy filterValues(GroupBy inputMap) {
        GroupByDelta delta = inputMap.delta();
        Map<Object, Object> outMap = outputCollection.toMap();

        if (delta.mode() == DeltaMode.RECOMPUTE_REQUIRED) {
            // No per-key changes available: full rescan, and over-approximate downstream as recompute.
            outputCollection.reset();
            Map<Object, Object> in = inputMap.toMap();
            for (Entry<Object, Object> e : in.entrySet()) {
                if (passes(e.getValue())) {
                    outMap.put(e.getKey(), e.getValue());
                }
            }
            outputCollection.setDelta(GroupByDelta.recomputeRequired());
            return outputCollection;
        }

        if (delta.mode() == DeltaMode.CLEAR_THEN_APPLY) {
            // Consumer clears its derived state, then we re-add only the passing replacement entries.
            outputCollection.reset();
            List outChanges = new ArrayList();
            List<Change> entries = delta.entries();
            for (int i = 0; i < entries.size(); i++) {
                Change c = entries.get(i);
                if (c.op() != ChangeOp.DELETE && passes(c.value())) {
                    outMap.put(c.key(), c.value());
                    outChanges.add(new Change(c.key(), c.value(), ChangeOp.ADD));
                }
            }
            outputCollection.setDelta(GroupByDelta.clearThenApply(outChanges));
            return outputCollection;
        }

        // INCREMENTAL: map each input change to its output change via the transition table.
        List outChanges = new ArrayList();
        List<Change> entries = delta.entries();
        for (int i = 0; i < entries.size(); i++) {
            Change c = entries.get(i);
            Object key = c.key();
            boolean wasIn = outMap.containsKey(key);
            switch (c.op()) {
                case ADD:
                case UPDATE: {
                    if (passes(c.value())) {
                        outMap.put(key, c.value());
                        outChanges.add(new Change(key, c.value(), wasIn ? ChangeOp.UPDATE : ChangeOp.ADD));
                    } else if (wasIn) {
                        Object previous = outMap.remove(key);
                        outChanges.add(Change.delete(key, previous));
                    }
                    break;
                }
                case DELETE: {
                    if (wasIn) {
                        Object previous = outMap.remove(key);
                        outChanges.add(Change.delete(key, previous));
                    }
                    break;
                }
                default:
                    throw new IllegalStateException("unknown ChangeOp " + c.op());
            }
        }
        outputCollection.setDelta(GroupByDelta.incremental(outChanges));
        return outputCollection;
    }

    private boolean passes(Object value) {
        return (boolean) mapFunction.apply(value);
    }
}
