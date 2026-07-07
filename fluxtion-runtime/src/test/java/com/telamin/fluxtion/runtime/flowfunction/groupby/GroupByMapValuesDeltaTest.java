/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * P2b: {@link GroupByMapFlowFunction#mapValues} is a 1:1 key-preserving transform, so its output
 * {@link GroupByDelta} mirrors the input's op/key with the value mapped. Each shape is pinned, then a
 * randomized {@link DeltaParityHarness} run asserts the incrementally-applied output delta equals a
 * full recompute every cycle.
 */
public class GroupByMapValuesDeltaTest {

    /** map value v -> v * 10. */
    private static final SerializableFunction<Integer, Integer> TIMES_10 = v -> v * 10;

    private static GroupByMapFlowFunction newMapper() {
        return new GroupByMapFlowFunction(TIMES_10);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static GroupByDelta mapOne(GroupByMapFlowFunction m, Change inputChange) {
        GroupByHashMap in = new GroupByHashMap();
        in.setDelta(GroupByDelta.incremental(Collections.singletonList(inputChange)));
        return m.mapValues(in).delta();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void addMapsValueAndEmitsAdd() {
        GroupByDelta d = mapOne(newMapper(), Change.add("A", 3));
        assertEquals(DeltaMode.INCREMENTAL, d.mode());
        assertEquals(Collections.singletonList(new Change<>("A", 30, ChangeOp.ADD)), d.entries());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void updateMapsValueAndEmitsUpdate() {
        GroupByMapFlowFunction m = newMapper();
        mapOne(m, Change.add("A", 3));
        GroupByDelta d = mapOne(m, Change.update("A", 7));
        assertEquals(Collections.singletonList(new Change<>("A", 70, ChangeOp.UPDATE)), d.entries());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void deleteRemovesAndEmitsDeleteWithMappedPreviousValue() {
        GroupByMapFlowFunction m = newMapper();
        mapOne(m, Change.add("A", 3)); // output holds A=30
        GroupByDelta d = mapOne(m, Change.delete("A"));
        assertEquals(Collections.singletonList(Change.delete("A", 30)), d.entries());
        // A really gone from the mapped view
        GroupByHashMap empty = new GroupByHashMap();
        empty.setDelta(GroupByDelta.incremental(Collections.emptyList()));
        assertEquals(0, m.mapValues(empty).toMap().size());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void clearThenApplyClearsAndReAddsMappedEntries() {
        GroupByMapFlowFunction m = newMapper();
        mapOne(m, Change.add("A", 1));
        GroupByHashMap in = new GroupByHashMap();
        List entries = new ArrayList();
        entries.add(new Change<>("B", 2, ChangeOp.ADD));
        in.setDelta(GroupByDelta.clearThenApply(entries));
        GroupBy out = m.mapValues(in);

        assertEquals(DeltaMode.CLEAR_THEN_APPLY, out.delta().mode());
        assertEquals(Collections.singletonList(new Change<>("B", 20, ChangeOp.ADD)), out.delta().entries());
        Map<Object, Object> expected = new HashMap<>();
        expected.put("B", 20);
        assertEquals(expected, out.toMap());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void recomputeRequiredRescansAndPropagatesRecompute() {
        GroupByMapFlowFunction m = newMapper();
        GroupByHashMap in = new GroupByHashMap();
        in.toMap().put("A", 3);
        in.toMap().put("B", 4);
        // default delta = RECOMPUTE_REQUIRED
        GroupBy out = m.mapValues(in);
        assertEquals(DeltaMode.RECOMPUTE_REQUIRED, out.delta().mode());
        Map<Object, Object> expected = new HashMap<>();
        expected.put("A", 30);
        expected.put("B", 40);
        assertEquals(expected, out.toMap());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void randomizedParityAgainstFullRecompute() {
        GroupByMapFlowFunction m = newMapper();
        DeltaParityHarness<Object, Object> harness = new DeltaParityHarness<>();
        Map<String, Integer> inputModel = new HashMap<>();
        String[] keys = {"A", "B", "C", "D", "E"};
        Random rnd = new Random(20260627L);

        for (int cycle = 0; cycle < 5000; cycle++) {
            String key = keys[rnd.nextInt(keys.length)];
            Change inputChange;
            boolean existing = inputModel.containsKey(key);
            if (existing && rnd.nextInt(5) == 0) {
                inputModel.remove(key);
                inputChange = Change.delete(key);
            } else {
                int value = rnd.nextInt(100);
                inputModel.put(key, value);
                inputChange = existing ? Change.update(key, value) : Change.add(key, value);
            }

            GroupByHashMap in = new GroupByHashMap();
            in.setDelta(GroupByDelta.incremental(Collections.singletonList(inputChange)));
            GroupBy out = m.mapValues(in);

            Map<Object, Object> recompute = new HashMap<>();
            for (Map.Entry<String, Integer> e : inputModel.entrySet()) {
                recompute.put(e.getKey(), e.getValue() * 10);
            }

            assertEquals("mapped view diverged at cycle " + cycle, recompute, out.toMap());
            harness.applyAndAssert(out.delta().copy(), recompute);
        }
    }
}
