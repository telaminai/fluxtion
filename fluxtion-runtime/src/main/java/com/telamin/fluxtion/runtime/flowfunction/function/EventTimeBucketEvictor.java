/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.function;

import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupByKey;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The memory-bounding companion to {@link EventTimeLatenessGate} for FSQL late-data windows (fsql-late-data.md
 * slice 2). The gate drops beyond-horizon <em>inputs</em>; this evictor removes the <em>materialized cells</em>
 * of buckets that have aged out, so the compound-key {@code (windowStart, key)} view stays bounded.
 *
 * <p>Placed on the bucketed flow and wired to {@code GroupByFlowBuilder.deleteByKey}:
 * {@code .deleteByKey(bucketedFlow.map(evictor::evictedKeys))}. Per bucketed row it tracks the max window-start
 * seen (a watermark <b>proxy</b>: {@code max(windowStart) ≤ max(eventTime) = the gate's W}, so the evictor never
 * purges a bucket the gate still accepts — consistent, and slightly conservative) and the live keys per
 * window-start, then returns the {@link GroupByKey}s of any bucket where {@code windowStart + size + L ≤ W}. The
 * returned keys are built with the SAME {@code keyFn} the group-by uses, so they compare equal and delete cleanly.
 *
 * @param <B> the bucketed record type ({@code <q>__Bucketed})
 */
public class EventTimeBucketEvictor<B> {

    private final long windowSizeMillis;
    private final long allowedLatenessMillis;
    private final SerializableFunction<B, Long> windowStartFn;
    private final SerializableFunction<B, GroupByKey<B>> keyFn;
    private transient long watermark = Long.MIN_VALUE;
    // Live keys per window-start. A Set (insertion-ordered for deterministic eviction emit-order) dedups the
    // repeated appends — a window receives one bucketed row per event, but many events share a (windowStart, key).
    private transient final Map<Long, Set<GroupByKey<B>>> keysByWindow = new LinkedHashMap<>();

    public EventTimeBucketEvictor(
            @AssignToField("windowSizeMillis") long windowSizeMillis,
            @AssignToField("allowedLatenessMillis") long allowedLatenessMillis,
            @AssignToField("windowStartFn") SerializableFunction<B, Long> windowStartFn,
            @AssignToField("keyFn") SerializableFunction<B, GroupByKey<B>> keyFn) {
        this.windowSizeMillis = windowSizeMillis;
        this.allowedLatenessMillis = allowedLatenessMillis;
        this.windowStartFn = windowStartFn;
        this.keyFn = keyFn;
    }

    /** Track this bucketed row and return the keys of any bucket that has now aged out (to be deleted). */
    public Collection<GroupByKey<B>> evictedKeys(B bucketed) {
        long windowStart = windowStartFn.apply(bucketed);
        if (windowStart > watermark) {
            watermark = windowStart;
        }
        keysByWindow.computeIfAbsent(windowStart, k -> new LinkedHashSet<>()).add(keyFn.apply(bucketed));
        List<GroupByKey<B>> evicted = new ArrayList<>();
        Iterator<Map.Entry<Long, Set<GroupByKey<B>>>> it = keysByWindow.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Set<GroupByKey<B>>> e = it.next();
            if (e.getKey() + windowSizeMillis + allowedLatenessMillis <= watermark) {
                evicted.addAll(e.getValue());
                it.remove();
            }
        }
        return evicted;
    }
}
