/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * P0 foundation: the {@link GroupByDelta} / {@link DeltaMode} / {@link Change} types, the canonical
 * {@link GroupByDelta#applyTo(Map)} contract, the per-cycle lifetime/copy rule, the backward-compatible
 * {@link GroupBy#delta()} default, and the delta-carrying {@link GroupByHashMap}. No producers/operators
 * yet (P1+).
 */
public class GroupByDeltaTest {

    @Test
    public void recomputeRequiredIsTheDefaultAndCannotBeAppliedIncrementally() {
        GroupByDelta<String, Integer> d = GroupByDelta.recomputeRequired();
        assertEquals(DeltaMode.RECOMPUTE_REQUIRED, d.mode());
        assertTrue(d.entries().isEmpty());
        assertThrows(IllegalStateException.class, () -> d.applyTo(new HashMap<>()));
        // shared singleton
        assertSame(GroupByDelta.recomputeRequired(), GroupByDelta.<Integer, Integer>recomputeRequired());
    }

    @Test
    public void incrementalAppliesAddUpdateDelete() {
        Map<String, Integer> derived = new HashMap<>();
        derived.put("A", 1);
        derived.put("B", 2);

        List<Change<String, Integer>> entries = new ArrayList<>();
        entries.add(Change.add("C", 3));        // new key
        entries.add(Change.update("A", 10));    // existing key
        entries.add(Change.delete("B"));        // remove

        GroupByDelta.incremental(entries).applyTo(derived);

        Map<String, Integer> expected = new HashMap<>();
        expected.put("A", 10);
        expected.put("C", 3);
        assertEquals(expected, derived);
    }

    @Test
    public void clearThenApplyClearsBeforeApplying() {
        Map<String, Integer> derived = new HashMap<>();
        derived.put("A", 1);
        derived.put("B", 2);

        List<Change<String, Integer>> basis = new ArrayList<>();
        basis.add(Change.add("X", 9));

        GroupByDelta.clearThenApply(basis).applyTo(derived);

        Map<String, Integer> expected = new HashMap<>();
        expected.put("X", 9);
        assertEquals(expected, derived);
    }

    @Test
    public void clearedEmptiesEverything() {
        Map<String, Integer> derived = new HashMap<>();
        derived.put("A", 1);

        GroupByDelta<String, Integer> cleared = GroupByDelta.cleared();
        assertEquals(DeltaMode.CLEAR_THEN_APPLY, cleared.mode());
        assertTrue(cleared.entries().isEmpty());

        cleared.applyTo(derived);
        assertTrue(derived.isEmpty());
    }

    @Test
    public void reusedChangeReflectsMutationButCopyIsStable() {
        Change<String, Integer> change = new Change<>("A", 1, ChangeOp.ADD);
        Change<String, Integer> snapshot = change.copy();

        change.set("B", 2, ChangeOp.UPDATE); // reuse in place (fast path / next cycle)

        assertEquals("B", change.key());
        assertEquals(Integer.valueOf(2), change.value());
        assertEquals(ChangeOp.UPDATE, change.op());

        // the copy is unaffected — safe to retain beyond the cycle
        assertNotSame(change, snapshot);
        assertEquals("A", snapshot.key());
        assertEquals(Integer.valueOf(1), snapshot.value());
        assertEquals(ChangeOp.ADD, snapshot.op());
    }

    @Test
    public void deltaCopyIsADeepSnapshot() {
        Change<String, Integer> reused = new Change<>("A", 1, ChangeOp.ADD);
        List<Change<String, Integer>> entries = new ArrayList<>();
        entries.add(reused);
        GroupByDelta<String, Integer> delta = GroupByDelta.incremental(entries);

        GroupByDelta<String, Integer> copy = delta.copy();

        // mutating the original's reused Change must not disturb the copy
        reused.set("Z", 99, ChangeOp.DELETE);
        assertEquals("A", copy.entries().get(0).key());
        assertEquals(Integer.valueOf(1), copy.entries().get(0).value());
        assertEquals(ChangeOp.ADD, copy.entries().get(0).op());

        // recomputeRequired copy is the immutable singleton
        assertSame(GroupByDelta.recomputeRequired(), GroupByDelta.<String, Integer>recomputeRequired().copy());
    }

    @Test
    public void groupByDefaultDeltaIsRecomputeRequired() {
        GroupBy<String, Integer> empty = GroupBy.emptyCollection();
        assertEquals(DeltaMode.RECOMPUTE_REQUIRED, empty.delta().mode());
    }

    @Test
    public void groupByHashMapCarriesDeltaAndResetsItToRecomputeRequired() {
        GroupByHashMap<String, Integer> gb = new GroupByHashMap<>();
        // default before any operator populates it: recompute (unchanged behaviour)
        assertEquals(DeltaMode.RECOMPUTE_REQUIRED, gb.delta().mode());

        List<Change<String, Integer>> entries = new ArrayList<>();
        entries.add(Change.add("A", 1));
        gb.setDelta(GroupByDelta.incremental(entries));
        assertEquals(DeltaMode.INCREMENTAL, gb.delta().mode());

        // reset() (called per operator cycle) wipes the delta back to the safe default
        gb.reset();
        assertEquals(DeltaMode.RECOMPUTE_REQUIRED, gb.delta().mode());
    }

    @Test
    public void addAndUpdateBothPutButTheOpIsPreserved() {
        // The core apply collapses ADD/UPDATE to put, but the op is retained for downstream consumers.
        Change<String, Integer> add = Change.add("A", 1);
        Change<String, Integer> update = Change.update("A", 1);
        assertFalse(add.op() == update.op());
        assertEquals(ChangeOp.ADD, add.op());
        assertEquals(ChangeOp.UPDATE, update.op());
    }
}
