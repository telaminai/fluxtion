/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive.IntSumFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * P3 layer 1: {@link GroupByFlowFunctionWrapper#combine}/{@link GroupByFlowFunctionWrapper#deduct}
 * accumulate the per-key changes of one window roll into a single multi-key {@link GroupByDelta}. A
 * roll issues combine(newBucket) then deduct(expiringBucket); reading {@code delta()} closes the cycle
 * so the next roll starts fresh.
 *
 * <p>The randomized oracle simulates a real {@code BucketedSlidingWindow} of an invertible sum reducer:
 * a ring of bucket aggregates, each roll combining the freshly-filled bucket and deducting the bucket
 * leaving the window. {@link DeltaParityHarness} asserts the accumulated delta, applied incrementally,
 * equals the window's full {@code toMap()} every roll.
 */
public class GroupByWindowDeltaTest {

    static final class Ev {
        final String k;
        final int v;

        Ev(String k, int v) {
            this.k = k;
            this.v = v;
        }

        String k() {
            return k;
        }

        int v() {
            return v;
        }
    }

    private static GroupByFlowFunctionWrapper<Ev, String, Integer, Integer, IntSumFlowFunction> newGroup() {
        SerializableFunction<Ev, String> key = Ev::k;
        SerializableFunction<Ev, Integer> value = Ev::v;
        SerializableSupplier<IntSumFlowFunction> agg = IntSumFlowFunction::new;
        return new GroupByFlowFunctionWrapper<>(key, value, agg);
    }

    @Test
    public void singleRollCombineThenDeductAccumulatesOneDelta() {
        GroupByFlowFunctionWrapper<Ev, String, Integer, Integer, IntSumFlowFunction> window = newGroup();

        // bucket entering the window: A=5, B=3
        GroupByFlowFunctionWrapper<Ev, String, Integer, Integer, IntSumFlowFunction> entering = newGroup();
        entering.aggregate(new Ev("A", 5));
        entering.aggregate(new Ev("B", 3));
        window.combine(entering);
        window.delta(); // close cycle 1 (warm-up: A=5,B=3)

        // next roll: a new bucket enters (A=2,C=9), and the first bucket (A=5,B=3) expires
        GroupByFlowFunctionWrapper<Ev, String, Integer, Integer, IntSumFlowFunction> entering2 = newGroup();
        entering2.aggregate(new Ev("A", 2));
        entering2.aggregate(new Ev("C", 9));
        window.combine(entering2);
        window.deduct(entering);

        GroupByDelta<String, Integer> d = window.delta();
        assertEquals(DeltaMode.INCREMENTAL, d.mode());
        // A: 5 +2 -5 = 2 (UPDATE); B: 3 -3 -> removed (DELETE); C: new 9 (ADD)
        Map<String, Change<String, Integer>> byKey = new HashMap<>();
        for (Change<String, Integer> c : d.entries()) {
            byKey.put(c.key(), c);
        }
        assertEquals(Integer.valueOf(2), byKey.get("A").value());
        assertEquals(ChangeOp.DELETE, byKey.get("B").op());
        assertEquals(new Change<>("C", 9, ChangeOp.ADD), byKey.get("C"));

        Map<String, Integer> expected = new HashMap<>();
        expected.put("A", 2);
        expected.put("C", 9);
        assertEquals(expected, window.toMap());
    }

    @Test
    public void randomizedSlidingWindowParity() {
        final int buckets = 4;
        GroupByFlowFunctionWrapper<Ev, String, Integer, Integer, IntSumFlowFunction> window = newGroup();
        // ring of filled buckets currently inside the window
        @SuppressWarnings("unchecked")
        GroupByFlowFunctionWrapper<Ev, String, Integer, Integer, IntSumFlowFunction>[] ring =
                new GroupByFlowFunctionWrapper[buckets];
        boolean[] live = new boolean[buckets];

        DeltaParityHarness<String, Integer> harness = new DeltaParityHarness<>();
        String[] keys = {"A", "B", "C", "D", "E"};
        Random rnd = new Random(20260627L);

        int writePointer = 0;
        for (int roll = 0; roll < 4000; roll++) {
            // build the bucket that is filling this period (0..3 events over the key space)
            GroupByFlowFunctionWrapper<Ev, String, Integer, Integer, IntSumFlowFunction> entering = newGroup();
            int events = rnd.nextInt(4);
            for (int e = 0; e < events; e++) {
                entering.aggregate(new Ev(keys[rnd.nextInt(keys.length)], rnd.nextInt(20) - 5));
            }

            // roll: combine the entering bucket, deduct the bucket leaving the ring slot
            window.combine(entering);
            if (live[writePointer]) {
                window.deduct(ring[writePointer]);
            }
            ring[writePointer] = entering;
            live[writePointer] = true;
            writePointer = (writePointer + 1) % buckets;

            harness.applyAndAssert(window.delta().copy(), new HashMap<>(window.toMap()));
        }
    }
}
