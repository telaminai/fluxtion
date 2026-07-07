/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy.KeyValue;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Proves the O(Δ) claim is real, not asserted: the delta-aware operators do work proportional to the
 * number of keys <em>changed this cycle</em> (Δ), not the total group size (N). The mechanism HAVING's
 * "correct" (delete-aware) lowering will ride on is {@code filterValues} + {@code changedKeyValues}, so
 * those are measured here by counting predicate invocations / emitted rows against a large group with a
 * single-key change.
 *
 * <p>The contrast with the {@link DeltaMode#RECOMPUTE_REQUIRED} fallback (which legitimately re-scans N)
 * pins that the saving comes specifically from the incremental path — a regression that silently
 * reverted an operator to full-recompute would flip these counts from O(1) back to O(N).
 */
public class GroupByDeltaComplexityTest {

    private static final int N = 1000; // group size; Δ = 1 changed key per cycle

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static GroupByHashMap groupOf(int size) {
        GroupByHashMap g = new GroupByHashMap();
        for (int i = 0; i < size; i++) {
            g.toMap().put("k" + i, i);
        }
        return g;
    }

    /** filterValues on an INCREMENTAL Δ=1 delta evaluates the predicate exactly once, independent of N. */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void filterValuesIncrementalIsODelta() {
        AtomicInteger predicateCalls = new AtomicInteger();
        SerializableFunction<Integer, Boolean> counting = v -> {
            predicateCalls.incrementAndGet();
            return v >= 0;
        };
        GroupByFilterFlowFunctionWrapper filter = new GroupByFilterFlowFunctionWrapper(counting);

        // Prime the filter's view with the full group via a RECOMPUTE_REQUIRED input (the bulk path).
        GroupByHashMap full = groupOf(N);
        full.setDelta(GroupByDelta.recomputeRequired());
        filter.filterValues(full);
        assertEquals("recompute legitimately scans all N", N, predicateCalls.get());

        // Now a single-key incremental change against the same N-key group.
        predicateCalls.set(0);
        GroupByHashMap step = groupOf(N);
        step.toMap().put("k500", 4242);
        step.setDelta(GroupByDelta.incremental(
                Collections.singletonList(new Change<>("k500", 4242, ChangeOp.UPDATE))));
        filter.filterValues(step);

        assertEquals("incremental filterValues must test only the changed key (O(Δ)), not all N",
                1, predicateCalls.get());
    }

    /** changedKeyValues on an INCREMENTAL Δ=1 delta emits one row, independent of N. */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void changedKeyValuesIncrementalEmitsODelta() {
        GroupByHashMap g = groupOf(N);
        g.setDelta(GroupByDelta.incremental(
                Collections.singletonList(new Change<>("k7", 99, ChangeOp.UPDATE))));

        List<KeyValue> emitted = GroupByDeltaFlowFunctions.changedKeyValues(g);
        assertEquals("changedKeyValues must emit only the changed key (O(Δ)), not all N",
                1, emitted.size());
        assertEquals("k7", emitted.get(0).getKey());

        // The RECOMPUTE_REQUIRED fallback legitimately emits all N (the bulk path).
        GroupByHashMap full = groupOf(N);
        full.setDelta(GroupByDelta.recomputeRequired());
        assertEquals(N, GroupByDeltaFlowFunctions.changedKeyValues(full).size());
    }

    /**
     * End-to-end of the HAVING-correct mechanism: a delta-aware {@code filterValues} (the aggregate
     * threshold) feeding {@code changedKeyValues} (the emission). A single-key change against a large
     * group does O(1) predicate work and emits at most one row — the per-event cost a windowed HAVING
     * inherits, independent of how many groups exist.
     */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void havingMechanismFilterThenChangedKeyValuesIsODelta() {
        AtomicInteger predicateCalls = new AtomicInteger();
        // HAVING sum(qty) >= 100, as a filter on the aggregate value
        SerializableFunction<Integer, Boolean> having = v -> {
            predicateCalls.incrementAndGet();
            return v >= 100;
        };
        GroupByFilterFlowFunctionWrapper filter = new GroupByFilterFlowFunctionWrapper(having);

        // prime with N qualifying groups
        GroupByHashMap full = groupOf(N);
        for (int i = 0; i < N; i++) {
            full.toMap().put("k" + i, 500);
        }
        full.setDelta(GroupByDelta.recomputeRequired());
        filter.filterValues(full);

        predicateCalls.set(0);
        // one group's windowed aggregate drops below the HAVING threshold -> should DELETE, O(1) work
        GroupByHashMap step = groupOf(N);
        for (int i = 0; i < N; i++) {
            step.toMap().put("k" + i, 500);
        }
        step.toMap().put("k123", 10); // now fails HAVING
        step.setDelta(GroupByDelta.incremental(
                Collections.singletonList(new Change<>("k123", 10, ChangeOp.UPDATE))));
        GroupBy filtered = filter.filterValues(step);

        assertEquals("HAVING predicate evaluated only for the changed group (O(Δ))", 1, predicateCalls.get());
        // delete-aware: the group that fell below the threshold is emitted as a DELETE (correct view)
        List<Change> changes = GroupByDeltaFlowFunctions.changes(filtered);
        assertEquals(1, changes.size());
        assertEquals(ChangeOp.DELETE, changes.get(0).op());
        assertEquals("k123", changes.get(0).key());
        // and the materialized filtered view dropped exactly that key
        assertTrue(filtered.toMap().containsKey("k0"));
        assertTrue(!filtered.toMap().containsKey("k123"));
    }
}
