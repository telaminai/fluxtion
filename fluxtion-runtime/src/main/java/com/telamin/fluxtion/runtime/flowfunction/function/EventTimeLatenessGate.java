/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.function;

import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.event.Event;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;

/**
 * A stateful event-time watermark gate for late-data (allowed-lateness) windowing. Used as a
 * {@code .filter(new EventTimeLatenessGate(size, L)::inHorizon)} in front of a compound-key
 * {@code (windowStart, key)} materialized group-by (FSQL {@code allowed lateness … on late retract},
 * fsql-late-data.md slice 2): it tracks the max event-time seen ({@code W = maxEventTime}) and drops an event
 * whose window bucket has fallen beyond the allowed-lateness horizon ({@code windowStart + size + L ≤ W}), so a
 * late event is accepted (and corrects its bucket cell) only while its bucket is still within {@code L} of the
 * watermark. Without this gate retention would be unbounded and {@code L} silently ignored.
 *
 * <p>Reads event-time from {@link Event#getEventTime()}, so one instance serves any event-time record (which
 * implements {@code Event}). The watermark is transient node state; the sizes are the persisted configuration.
 *
 * @param <B> the bucketed record type ({@code <q>__Bucketed}) for the HOP per-bucket horizon check; the event
 *            branch ({@link #inHorizon}/{@link #beyondHorizon}) is independent of {@code B}.
 */
public class EventTimeLatenessGate<B> {

    private final long stepMillis;        // window advance: slide for HOP, size for TUMBLE
    private final long windowSizeMillis;
    private final long allowedLatenessMillis;
    private final SerializableFunction<B, Long> bucketWindowStartFn; // HOP per-bucket horizon; unused for TUMBLE
    private transient long watermark = Long.MIN_VALUE;

    public EventTimeLatenessGate(
            @AssignToField("stepMillis") long stepMillis,
            @AssignToField("windowSizeMillis") long windowSizeMillis,
            @AssignToField("allowedLatenessMillis") long allowedLatenessMillis,
            @AssignToField("bucketWindowStartFn") SerializableFunction<B, Long> bucketWindowStartFn) {
        this.stepMillis = stepMillis;
        this.windowSizeMillis = windowSizeMillis;
        this.allowedLatenessMillis = allowedLatenessMillis;
        this.bucketWindowStartFn = bucketWindowStartFn;
    }

    /**
     * Advances the watermark with this event's event-time and returns whether the event's bucket is still within
     * the allowed-lateness horizon. {@code true} = pass (aggregate/correct its cell); {@code false} = drop
     * (beyond horizon).
     */
    public boolean inHorizon(Event event) {
        long eventTime = event.getEventTime();
        if (eventTime > watermark) {
            watermark = eventTime;
        }
        long latestWindowStart = (eventTime / stepMillis) * stepMillis;
        return latestWindowStart + windowSizeMillis + allowedLatenessMillis > watermark;
    }

    /**
     * The complement of {@link #inHorizon} for the {@code on late route to <sink>} side branch: true when the
     * event's bucket is beyond the allowed-lateness horizon (so it should be routed to the late-data sink rather
     * than aggregated). Read-only w.r.t. the watermark — {@link #inHorizon} on the main branch advances it. The
     * current event's own bucket is never beyond-horizon relative to itself, so branch ordering is safe.
     */
    public boolean beyondHorizon(Event event) {
        long eventTime = event.getEventTime();
        long latestWindowStart = (eventTime / stepMillis) * stepMillis;
        long w = Math.max(watermark, eventTime);
        return latestWindowStart + windowSizeMillis + allowedLatenessMillis <= w;
    }

    /**
     * Per-bucket horizon check for a HOP (sliding) fan-out: a late event fans out to <em>every</em> overlapping
     * window, but {@link #inHorizon} only decides on the event's <em>latest</em> window (enough to admit the
     * event). Each fanned-out {@code <q>__Bucketed} row is then filtered here against its OWN window's horizon, so
     * an earlier overlapping window that is already beyond {@code L} is dropped rather than resurrected. Read-only
     * w.r.t. the watermark — {@link #inHorizon} on the upstream event branch advances it first (the bucket flow is
     * downstream of that gate), so {@code watermark} is current. Exact for TUMBLE too (one bucket, same horizon as
     * the event), where it is simply not wired.
     */
    public boolean bucketInHorizon(B bucket) {
        long windowStart = bucketWindowStartFn.apply(bucket);
        return windowStart + windowSizeMillis + allowedLatenessMillis > watermark;
    }
}
