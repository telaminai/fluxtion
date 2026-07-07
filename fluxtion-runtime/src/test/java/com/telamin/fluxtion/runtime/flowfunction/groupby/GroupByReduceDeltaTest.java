/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive.IntMaxFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive.IntSumFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * P2c: {@link GroupByReduceFlowFunction} collapses a group to a scalar. For an invertible reducer
 * ({@code deductSupported()} = sum/count/average) it maintains the running aggregate in O(Δ) from the
 * input delta (deduct old value + add new); a non-invertible reducer (min/max) always recomputes. The
 * randomized parity oracle asserts the incremental scalar equals a full recompute every cycle.
 */
public class GroupByReduceDeltaTest {

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static GroupByReduceFlowFunction sumReducer() {
        SerializableSupplier<IntSumFlowFunction> s = IntSumFlowFunction::new;
        return new GroupByReduceFlowFunction(s);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static GroupByReduceFlowFunction maxReducer() {
        SerializableSupplier<IntMaxFlowFunction> s = IntMaxFlowFunction::new;
        return new GroupByReduceFlowFunction(s);
    }

    /** A GroupBy whose toMap reflects {@code model} and whose delta is a single incremental change. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static GroupByHashMap input(Map<String, Integer> model, Change change) {
        GroupByHashMap in = new GroupByHashMap();
        in.toMap().putAll(model);
        in.setDelta(GroupByDelta.incremental(Collections.singletonList(change)));
        return in;
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void invertibleSumMaintainedIncrementally() {
        GroupByReduceFlowFunction r = sumReducer();
        Map<String, Integer> m = new HashMap<>();

        m.put("A", 5);
        assertEquals(Integer.valueOf(5), r.reduceValues(input(m, Change.add("A", 5))));
        m.put("B", 3);
        assertEquals(Integer.valueOf(8), r.reduceValues(input(m, Change.add("B", 3))));
        m.put("A", 10);
        assertEquals(Integer.valueOf(13), r.reduceValues(input(m, Change.update("A", 10)))); // deduct 5, add 10
        m.remove("B");
        assertEquals(Integer.valueOf(10), r.reduceValues(input(m, Change.delete("B"))));      // deduct 3
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void nonInvertibleMaxAlwaysRecomputesFromToMap() {
        GroupByReduceFlowFunction r = maxReducer();
        Map<String, Integer> m = new HashMap<>();

        m.put("A", 5);
        assertEquals(Integer.valueOf(5), r.reduceValues(input(m, Change.add("A", 5))));
        m.put("B", 9);
        assertEquals(Integer.valueOf(9), r.reduceValues(input(m, Change.add("B", 9))));
        // B drops out: max must fall back to 5 — only possible by recompute, not by deduct
        m.remove("B");
        assertEquals(Integer.valueOf(5), r.reduceValues(input(m, Change.delete("B"))));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void recomputeRequiredDeltaRescans() {
        GroupByReduceFlowFunction r = sumReducer();
        GroupByHashMap in = new GroupByHashMap();
        in.toMap().put("A", 5);
        in.toMap().put("B", 7);
        // default delta = RECOMPUTE_REQUIRED
        assertEquals(Integer.valueOf(12), r.reduceValues(in));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void randomizedSumParityAgainstFullRecompute() {
        GroupByReduceFlowFunction r = sumReducer();
        Map<String, Integer> model = new HashMap<>();
        String[] keys = {"A", "B", "C", "D", "E"};
        Random rnd = new Random(20260627L);

        for (int cycle = 0; cycle < 5000; cycle++) {
            String key = keys[rnd.nextInt(keys.length)];
            Change change;
            boolean existing = model.containsKey(key);
            if (existing && rnd.nextInt(5) == 0) {
                model.remove(key);
                change = Change.delete(key);
            } else {
                int value = rnd.nextInt(50) - 10; // negatives too — exercises real deduct
                model.put(key, value);
                change = existing ? Change.update(key, value) : Change.add(key, value);
            }

            Object scalar = r.reduceValues(input(model, change));

            int expected = 0;
            for (int v : model.values()) {
                expected += v;
            }
            assertEquals("reduce scalar diverged at cycle " + cycle, expected, ((Number) scalar).intValue());
        }
    }
}
