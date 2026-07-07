/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The set of key changes a {@link GroupBy} produced in the current event cycle — a changelog that lets
 * downstream operators (and FSQL typed output) update in O(Δ) instead of re-scanning the whole group.
 *
 * <p>Model: a {@link DeltaMode} plus an ordered list of {@link Change} entries. The canonical consumer
 * is {@link #applyTo(Map)}; the three modes and the "full clear = {@link DeltaMode#CLEAR_THEN_APPLY}
 * with no entries" rule are described in {@code docs/design/groupby-delta-ivm.md}.
 *
 * <p>Backward compatibility: {@link GroupBy#delta()} defaults to {@link #recomputeRequired()}, so a
 * producer that does not yet emit deltas simply forces consumers to recompute — a delta is an
 * optimisation, never a correctness dependency.
 *
 * <p>See {@link Change} for the per-cycle lifetime rule; use {@link #copy()} to retain a delta beyond
 * the current cycle.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class GroupByDelta<K, V> {

    private static final GroupByDelta<Object, Object> RECOMPUTE_REQUIRED =
            new GroupByDelta<>(DeltaMode.RECOMPUTE_REQUIRED, Collections.emptyList());

    private final DeltaMode mode;
    private final List<Change<K, V>> entries;

    private GroupByDelta(DeltaMode mode, List<Change<K, V>> entries) {
        this.mode = mode;
        this.entries = entries;
    }

    /** Shared "no delta info" instance — the consumer must recompute/diff from {@link GroupBy#toMap()}. */
    @SuppressWarnings("unchecked")
    public static <K, V> GroupByDelta<K, V> recomputeRequired() {
        return (GroupByDelta<K, V>) (GroupByDelta<?, ?>) RECOMPUTE_REQUIRED;
    }

    /** Entries applied on top of the existing derived state. */
    public static <K, V> GroupByDelta<K, V> incremental(List<Change<K, V>> entries) {
        return new GroupByDelta<>(DeltaMode.INCREMENTAL, entries);
    }

    /** Derived state is cleared, then the entries are applied as the new basis. */
    public static <K, V> GroupByDelta<K, V> clearThenApply(List<Change<K, V>> entries) {
        return new GroupByDelta<>(DeltaMode.CLEAR_THEN_APPLY, entries);
    }

    /** A full clear with no replacement entries — {@link DeltaMode#CLEAR_THEN_APPLY} with an empty list. */
    public static <K, V> GroupByDelta<K, V> cleared() {
        return new GroupByDelta<>(DeltaMode.CLEAR_THEN_APPLY, Collections.<Change<K, V>>emptyList());
    }

    public DeltaMode mode() {
        return mode;
    }

    public List<Change<K, V>> entries() {
        return entries;
    }

    /**
     * Apply this delta to a consumer's derived map, embodying the one-API/three-mode contract.
     * {@link DeltaMode#RECOMPUTE_REQUIRED} cannot be applied incrementally — the caller must recompute
     * or diff from {@link GroupBy#toMap()} (calling this throws to surface the contract violation).
     */
    public void applyTo(Map<K, V> derivedState) {
        if (mode == DeltaMode.RECOMPUTE_REQUIRED) {
            throw new IllegalStateException(
                    "RECOMPUTE_REQUIRED delta cannot be applied incrementally — recompute/diff from toMap()");
        }
        if (mode == DeltaMode.CLEAR_THEN_APPLY) {
            derivedState.clear();
        }
        for (int i = 0; i < entries.size(); i++) {
            Change<K, V> change = entries.get(i);
            switch (change.op()) {
                case ADD:
                case UPDATE:
                    derivedState.put(change.key(), change.value());
                    break;
                case DELETE:
                    derivedState.remove(change.key());
                    break;
                default:
                    throw new IllegalStateException("unknown ChangeOp " + change.op());
            }
        }
    }

    /** An independent deep snapshot (each {@link Change} copied), safe to retain beyond the cycle. */
    public GroupByDelta<K, V> copy() {
        if (mode == DeltaMode.RECOMPUTE_REQUIRED) {
            return this; // immutable singleton
        }
        List<Change<K, V>> copied = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            copied.add(entries.get(i).copy());
        }
        return new GroupByDelta<>(mode, copied);
    }

    @Override
    public String toString() {
        return "GroupByDelta{" + mode + ", entries=" + entries + '}';
    }
}
