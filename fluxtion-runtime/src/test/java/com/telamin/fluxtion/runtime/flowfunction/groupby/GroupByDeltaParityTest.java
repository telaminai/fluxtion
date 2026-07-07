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
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * P0 oracle self-test: over a long randomized sequence of ADD/UPDATE/DELETE/CLEAR/CLEAR_THEN_APPLY and
 * RECOMPUTE_REQUIRED cycles, the state obtained by applying {@link GroupByDelta}s incrementally
 * (via {@link DeltaParityHarness}) must always equal an independently-maintained authoritative map.
 *
 * <p>The authoritative map is mutated by direct {@code put}/{@code remove}; the delta is built from the
 * same operations but applied through {@link GroupByDelta#applyTo(Map)} to a separate map — so this
 * proves the apply contract against direct mutation. P1+ operators reuse {@link DeltaParityHarness}
 * with their own delta vs full-recompute output.
 */
public class GroupByDeltaParityTest {

    private static final int CYCLES = 5_000;
    private static final int KEY_SPACE = 20;   // small, so updates/deletes frequently hit live keys

    @Test
    public void incrementalApplicationStaysInParityWithDirectMutation() {
        Random rng = new Random(20260627L);
        Map<Integer, Integer> truth = new HashMap<>();
        DeltaParityHarness<Integer, Integer> harness = new DeltaParityHarness<>();

        for (int cycle = 0; cycle < CYCLES; cycle++) {
            int kind = rng.nextInt(100);
            GroupByDelta<Integer, Integer> delta;

            if (kind < 6) {
                // full clear
                truth.clear();
                delta = GroupByDelta.cleared();
            } else if (kind < 12) {
                // clear then apply a fresh basis
                truth.clear();
                List<Change<Integer, Integer>> basis = new ArrayList<>();
                int n = 1 + rng.nextInt(4);
                for (int i = 0; i < n; i++) {
                    int k = rng.nextInt(KEY_SPACE);
                    int v = rng.nextInt(1000);
                    if (!truth.containsKey(k)) {        // basis entries are all ADDs
                        truth.put(k, v);
                        basis.add(Change.add(k, v));
                    }
                }
                delta = GroupByDelta.clearThenApply(basis);
            } else if (kind < 18) {
                // producer with no delta — exercise the RECOMPUTE_REQUIRED fallback
                int k = rng.nextInt(KEY_SPACE);
                if (rng.nextBoolean()) {
                    truth.put(k, rng.nextInt(1000));
                } else {
                    truth.remove(k);
                }
                delta = GroupByDelta.recomputeRequired();
            } else {
                // an incremental batch of puts/removes
                List<Change<Integer, Integer>> entries = new ArrayList<>();
                int ops = 1 + rng.nextInt(5);
                for (int i = 0; i < ops; i++) {
                    int k = rng.nextInt(KEY_SPACE);
                    if (rng.nextInt(3) == 0) {
                        // remove
                        if (truth.containsKey(k)) {
                            truth.remove(k);
                            entries.add(Change.delete(k));
                        }
                    } else {
                        // put — ADD if new, UPDATE if present (computed before mutation)
                        int v = rng.nextInt(1000);
                        ChangeOp op = truth.containsKey(k) ? ChangeOp.UPDATE : ChangeOp.ADD;
                        truth.put(k, v);
                        entries.add(new Change<>(k, v, op));
                    }
                }
                delta = GroupByDelta.incremental(entries);
            }

            harness.applyAndAssert(delta, new HashMap<>(truth));
        }

        assertTrue("harness ran the full sequence", harness.cycles() == CYCLES);
    }
}
