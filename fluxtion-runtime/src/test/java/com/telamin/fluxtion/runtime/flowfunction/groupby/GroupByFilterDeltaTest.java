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
 * P2a: {@link GroupByFilterFlowFunctionWrapper} maintains the filtered view incrementally and emits an
 * output {@link GroupByDelta} per the §5 transition table. This pins each row directly, then runs a
 * randomized parity oracle ({@link DeltaParityHarness}) asserting the incrementally-applied output
 * delta stream equals a full recompute on every cycle — the bug surface the windowing/IVM fix targets.
 */
public class GroupByFilterDeltaTest {

    /** value passes the filter iff it is >= 10. */
    private static final SerializableFunction<Integer, Boolean> GTE_10 = v -> v >= 10;

    private static GroupByFilterFlowFunctionWrapper newFilter() {
        return new GroupByFilterFlowFunctionWrapper(GTE_10);
    }

    /** Drive the wrapper with one input change carrying the given op, return the emitted output delta. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static GroupByDelta filterOne(GroupByFilterFlowFunctionWrapper filter, Change inputChange) {
        GroupByHashMap in = new GroupByHashMap();
        in.setDelta(GroupByDelta.incremental(Collections.singletonList(inputChange)));
        GroupBy out = filter.filterValues(in);
        return out.delta();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void addPassingEmitsAdd() {
        GroupByDelta d = filterOne(newFilter(), Change.add("A", 12));
        assertEquals(DeltaMode.INCREMENTAL, d.mode());
        assertEquals(Collections.singletonList(new Change<>("A", 12, ChangeOp.ADD)), d.entries());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void addFailingEmitsNothing() {
        GroupByDelta d = filterOne(newFilter(), Change.add("A", 3));
        assertEquals(0, d.entries().size());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void updateOutToInEmitsAdd() {
        GroupByFilterFlowFunctionWrapper f = newFilter();
        filterOne(f, Change.add("A", 3));                 // fails -> not in output
        GroupByDelta d = filterOne(f, Change.update("A", 11)); // now passes
        assertEquals(Collections.singletonList(new Change<>("A", 11, ChangeOp.ADD)), d.entries());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void updateInToInEmitsUpdate() {
        GroupByFilterFlowFunctionWrapper f = newFilter();
        filterOne(f, Change.add("A", 12));                 // passes -> in output
        GroupByDelta d = filterOne(f, Change.update("A", 20)); // still passes
        assertEquals(Collections.singletonList(new Change<>("A", 20, ChangeOp.UPDATE)), d.entries());
    }

    /** The classic IVM bug row: a key already in the view updates to a value that no longer passes. */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void updateInToOutEmitsDelete() {
        GroupByFilterFlowFunctionWrapper f = newFilter();
        filterOne(f, Change.add("A", 12));                 // passes -> in output
        GroupByDelta d = filterOne(f, Change.update("A", 4)); // now fails
        assertEquals(Collections.singletonList(Change.delete("A", 12)), d.entries());
        assertEquals(0, f.filterValues(emptyIncremental()).toMap().size()); // A really removed
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void updateOutToOutEmitsNothing() {
        GroupByFilterFlowFunctionWrapper f = newFilter();
        filterOne(f, Change.add("A", 3));                  // fails
        GroupByDelta d = filterOne(f, Change.update("A", 5)); // still fails
        assertEquals(0, d.entries().size());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void deleteInEmitsDelete() {
        GroupByFilterFlowFunctionWrapper f = newFilter();
        filterOne(f, Change.add("A", 12));
        GroupByDelta d = filterOne(f, Change.delete("A"));
        assertEquals(Collections.singletonList(Change.delete("A", 12)), d.entries());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void deleteOutEmitsNothing() {
        GroupByFilterFlowFunctionWrapper f = newFilter();
        filterOne(f, Change.add("A", 3)); // never in output
        GroupByDelta d = filterOne(f, Change.delete("A"));
        assertEquals(0, d.entries().size());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void clearThenApplyClearsAndReAddsPassingEntries() {
        GroupByFilterFlowFunctionWrapper f = newFilter();
        filterOne(f, Change.add("A", 12));
        filterOne(f, Change.add("B", 50));

        GroupByHashMap in = new GroupByHashMap();
        List entries = new ArrayList();
        entries.add(new Change<>("C", 30, ChangeOp.ADD)); // passes
        entries.add(new Change<>("D", 2, ChangeOp.ADD));  // fails
        in.setDelta(GroupByDelta.clearThenApply(entries));
        GroupBy out = f.filterValues(in);

        assertEquals(DeltaMode.CLEAR_THEN_APPLY, out.delta().mode());
        assertEquals(Collections.singletonList(new Change<>("C", 30, ChangeOp.ADD)), out.delta().entries());
        Map<Object, Object> expected = new HashMap<>();
        expected.put("C", 30);
        assertEquals(expected, out.toMap());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void recomputeRequiredRescansAndPropagatesRecompute() {
        GroupByFilterFlowFunctionWrapper f = newFilter();
        GroupByHashMap in = new GroupByHashMap();
        in.toMap().put("A", 12); // passes
        in.toMap().put("B", 4);  // fails
        // default delta is RECOMPUTE_REQUIRED
        GroupBy out = f.filterValues(in);

        assertEquals(DeltaMode.RECOMPUTE_REQUIRED, out.delta().mode());
        Map<Object, Object> expected = new HashMap<>();
        expected.put("A", 12);
        assertEquals(expected, out.toMap());
    }

    /**
     * Randomized parity: a single-key change stream drives the wrapper; the output delta applied
     * incrementally MUST equal a full recompute of {@code filter(inputMap)} every cycle.
     */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void randomizedParityAgainstFullRecompute() {
        GroupByFilterFlowFunctionWrapper f = newFilter();
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
                int value = rnd.nextInt(25); // straddles the >=10 threshold
                inputModel.put(key, value);
                inputChange = existing ? Change.update(key, value) : Change.add(key, value);
            }

            GroupByHashMap in = new GroupByHashMap();
            in.setDelta(GroupByDelta.incremental(Collections.singletonList(inputChange)));
            GroupBy out = f.filterValues(in);

            Map<Object, Object> recompute = new HashMap<>();
            for (Map.Entry<String, Integer> e : inputModel.entrySet()) {
                if (e.getValue() >= 10) {
                    recompute.put(e.getKey(), e.getValue());
                }
            }

            assertEquals("wrapper view diverged at cycle " + cycle, recompute, out.toMap());
            harness.applyAndAssert(out.delta().copy(), recompute);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static GroupByHashMap emptyIncremental() {
        GroupByHashMap in = new GroupByHashMap();
        in.setDelta(GroupByDelta.incremental(Collections.emptyList()));
        return in;
    }
}
