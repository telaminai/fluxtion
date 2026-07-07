/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy.KeyValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stateless flat-map helpers that turn a {@link GroupBy}'s per-cycle {@link GroupByDelta} into a
 * stream of changed entries — the runtime side of {@code GroupByFlowBuilder.changedKeyValues()} /
 * {@code changes()}. Used as serializable method references in {@code flatMap}, so AOT-friendly.
 *
 * <p><b>Why the signatures are raw.</b> These are emitted by the generator as bare method references
 * ({@code GroupByDeltaFlowFunctions::changedKeyValues}) into {@code new FlatMapFlowFunction<>(rawUpstream, ref)}.
 * A <em>generic</em> static method there leaves javac unable to infer the diamond from a raw upstream
 * (it has nothing to pin {@code K,V} to). A non-generic method ref fully determines the
 * {@code SerializableFunction} type on its own — the same reason {@code csvToIterable(String)} lowers
 * cleanly. The builder re-attaches the {@code <K,V>} façade (one unchecked cast at the seam).
 *
 * <p>These flatten the per-key entries; they do not carry the {@link DeltaMode} itself (a stream
 * consumer has no derived GroupBy state to clear — the mode matters to delta-aware <em>operators</em>,
 * P2). When the producer offers no delta ({@link DeltaMode#RECOMPUTE_REQUIRED}) the safe
 * over-approximation is to emit every current entry (never drop a change); a stateful diff is a later
 * optimisation.
 */
public final class GroupByDeltaFlowFunctions {

    private GroupByDeltaFlowFunctions() {
    }

    /**
     * The ADD/UPDATE entries as {@link KeyValue}s (DELETEs excluded) — the append/upsert path safe to
     * feed into a typed-row mapper.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<KeyValue> changedKeyValues(GroupBy groupBy) {
        GroupByDelta delta = groupBy.delta();
        List<KeyValue> out = new ArrayList<>();
        if (delta.mode() == DeltaMode.RECOMPUTE_REQUIRED) {
            Map<Object, Object> map = groupBy.toMap();
            for (Map.Entry<Object, Object> e : map.entrySet()) {
                out.add(new KeyValue(e.getKey(), e.getValue()));
            }
            return out;
        }
        List<Change> entries = delta.entries();
        for (int i = 0; i < entries.size(); i++) {
            Change change = entries.get(i);
            if (change.op() != ChangeOp.DELETE) {
                out.add(new KeyValue(change.key(), change.value()));
            }
        }
        return out;
    }

    /**
     * The full {@link Change} stream including DELETE — the delete-aware path for materialised views /
     * actions. Entries are copied so a downstream that buffers them is unaffected by the producer
     * reusing a single-{@link Change} slot on the next cycle.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<Change> changes(GroupBy groupBy) {
        GroupByDelta delta = groupBy.delta();
        if (delta.mode() == DeltaMode.RECOMPUTE_REQUIRED) {
            List<Change> out = new ArrayList<>();
            Map<Object, Object> map = groupBy.toMap();
            for (Map.Entry<Object, Object> e : map.entrySet()) {
                out.add(Change.update(e.getKey(), e.getValue()));
            }
            return out;
        }
        List<Change> entries = delta.entries();
        List<Change> out = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            out.add(entries.get(i).copy());
        }
        return out;
    }
}
