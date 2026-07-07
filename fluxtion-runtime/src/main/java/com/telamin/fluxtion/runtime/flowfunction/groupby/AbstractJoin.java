/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.flowfunction.Stateful;
import com.telamin.fluxtion.runtime.flowfunction.Tuple;
import com.telamin.fluxtion.runtime.util.ObjectPool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Base for the keyed joins (inner/left/right/outer). Maintains the joined map <b>incrementally</b> and
 * publishes a <b>precise {@link GroupByDelta}</b> of only the keys whose joined value actually changed this
 * cycle — so a downstream {@code changedKeyValues()}/{@code changes()} emits exactly the changed keys, not
 * the whole map.
 *
 * <p><b>Why recompute-and-compare (not firing-side detection).</b> A join has two parents; on any trigger it
 * is handed both current maps but cannot tell which one just fired (the other side's {@link GroupByDelta} is
 * stale). So instead of trusting a delta blindly, it takes the <b>union</b> of both deltas' keys as
 * <i>candidates</i> (or every key when an input offers no precise delta, e.g. a windowed combine), recomputes
 * each candidate's joined value, and emits a change only when it differs from the stored value. A stale
 * candidate recomputes to the same value → no spurious emit. Work is O(Δ) in the common precise-delta case.
 *
 * <p>Subclasses supply only {@link #included(boolean, boolean)} — the membership rule for a key given which
 * sides are present. The joined value is always the pair {@code (leftValue, rightValue)} (either may be null
 * for an outer join).
 */
public abstract class AbstractJoin implements Stateful<GroupBy> {

    protected final transient GroupByHashMap<Object, MutableTuple<Object, Object>> joinedGroup = new GroupByHashMap<>();
    protected final transient ObjectPool<MutableTuple> tupleObjectPool = new ObjectPool<>(MutableTuple::new);

    /** Whether a key with the given left/right presence belongs in this join's output. */
    protected abstract boolean included(boolean leftPresent, boolean rightPresent);

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <K1, V1, K2 extends K1, V2> GroupBy<K1, Tuple<V1, V2>> join(
            GroupBy<K1, V1> leftGroupBy, GroupBy<K2, V2> rightGroupBY) {
        Map leftMap = leftGroupBy == null ? Collections.emptyMap() : leftGroupBy.toMap();
        Map rightMap = rightGroupBY == null ? Collections.emptyMap() : rightGroupBY.toMap();
        Map<Object, MutableTuple<Object, Object>> joined = joinedGroup.toMap();

        Set<Object> candidates = new LinkedHashSet<>();
        boolean recompute = collectDeltaKeys(candidates, leftGroupBy) | collectDeltaKeys(candidates, rightGroupBY);
        if (recompute) {
            candidates.clear();
            candidates.addAll(leftMap.keySet());
            candidates.addAll(rightMap.keySet());
            candidates.addAll(joined.keySet()); // so a key that vanished from both inputs is removed
        }

        List<Change> changes = new ArrayList<>();
        for (Object key : candidates) {
            Object leftV = leftMap.get(key);
            Object rightV = rightMap.get(key);
            boolean include = included(leftV != null, rightV != null);
            MutableTuple existing = joined.get(key);
            if (!include) {
                if (existing != null) {
                    joined.remove(key);
                    existing.returnToPool(tupleObjectPool);
                    changes.add(Change.delete(key));
                }
            } else if (existing == null) {
                MutableTuple t = tupleObjectPool.checkOut().setFirst(leftV).setSecond(rightV);
                joined.put(key, t);
                changes.add(Change.add(key, t));
            } else if (!Objects.equals(existing.getFirst(), leftV) || !Objects.equals(existing.getSecond(), rightV)) {
                existing.setFirst(leftV).setSecond(rightV);
                changes.add(Change.update(key, existing));
            }
            // else: candidate recomputed to the same joined value (e.g. a stale-delta key) — no emit.
        }
        joinedGroup.setDelta(GroupByDelta.incremental((List) changes));
        return (GroupBy<K1, Tuple<V1, V2>>) (Object) joinedGroup;
    }

    /** Adds the keys changed in {@code gb}'s delta to {@code out}; returns true if {@code gb} forces a full rescan. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean collectDeltaKeys(Set<Object> out, GroupBy gb) {
        if (gb == null) {
            return false;
        }
        GroupByDelta delta = gb.delta();
        if (delta.mode() == DeltaMode.RECOMPUTE_REQUIRED) {
            return true;
        }
        List<Change> entries = delta.entries();
        for (int i = 0; i < entries.size(); i++) {
            out.add(entries.get(i).key());
        }
        return false;
    }

    //hack for incomplete generics in generated code
    @SuppressWarnings({"rawtypes", "unchecked"})
    public GroupBy join(Object leftGroupBy, Object rightGroupBY) {
        return this.join((GroupBy) leftGroupBy, (GroupBy) rightGroupBY);
    }

    @Override
    public GroupBy reset() {
        joinedGroup.values().forEach(t -> t.returnToPool(tupleObjectPool));
        return joinedGroup.reset();
    }
}
