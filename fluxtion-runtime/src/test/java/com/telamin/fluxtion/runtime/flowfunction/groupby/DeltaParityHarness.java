/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Reusable parity oracle for delta-driven incremental view maintenance.
 *
 * <p>The contract every delta-aware producer/operator must satisfy: the state obtained by applying the
 * stream of {@link GroupByDelta}s incrementally MUST equal the state obtained by a full recompute, on
 * every cycle. Feed this harness {@code (delta, authoritativeRecompute)} per cycle; it maintains the
 * incrementally-applied state and asserts the two agree — failing loudly on the first cycle they
 * diverge.
 *
 * <p>P0 self-tests it against direct map mutation; P1+ operators feed it their delta output paired with
 * their full-recompute output.
 */
public final class DeltaParityHarness<K, V> {

    private final Map<K, V> deltaApplied = new HashMap<>();
    private int cycle = 0;

    /**
     * Apply {@code delta} to the running incremental state and assert it matches the authoritative
     * recompute. A {@link DeltaMode#RECOMPUTE_REQUIRED} delta is honoured by replacing the running
     * state with the recompute (the consumer's mandated fallback).
     */
    public void applyAndAssert(GroupByDelta<K, V> delta, Map<K, V> authoritativeRecompute) {
        cycle++;
        if (delta.mode() == DeltaMode.RECOMPUTE_REQUIRED) {
            deltaApplied.clear();
            deltaApplied.putAll(authoritativeRecompute);
        } else {
            delta.applyTo(deltaApplied);
        }
        assertEquals("delta-applied state diverged from recompute at cycle " + cycle,
                authoritativeRecompute, deltaApplied);
    }

    public Map<K, V> state() {
        return deltaApplied;
    }

    public int cycles() {
        return cycle;
    }
}
