/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.time;

import lombok.Getter;

/**
 * @author 2024 gregory higgins.
 */
public interface ClockStrategy {

    long getWallClockTime();

    static ClockStrategyEvent registerClockEvent(ClockStrategy clock) {
        return new ClockStrategyEvent(clock);
    }

    /**
     * A monotonic clock reporting <b>nanoseconds since the epoch</b>, anchored once at construction.
     *
     * <p>The default strategy is {@code System::currentTimeMillis}. Measured on an Apple M4 it costs
     * <b>12.9 ns</b> per call against <b>8.0 ns</b> for {@code System.nanoTime()}, and the audited path
     * reads a clock twice per event — once in {@link Clock#eventReceived}, once for the record's
     * {@code endTime}. That is ~26 ns/event.
     *
     * <p>The resolution matters more than the cost. {@code currentTimeMillis} advances 1000 times a
     * second, so on any event faster than a millisecond {@code endTime - logTime} — the field that
     * exists to report processing duration — is <b>always zero</b>. The framework pays twice per event
     * for a number that cannot be non-zero.
     *
     * <p>This strategy anchors {@code System.currentTimeMillis()} against {@code System.nanoTime()} once
     * and advances by the monotonic delta thereafter, so timestamps stay comparable to wall-clock time
     * while durations become real. It is monotonic — it will not step backwards over an NTP correction,
     * which {@code currentTimeMillis} can.
     *
     * <p><b>The unit changes.</b> This returns nanoseconds where the default returns milliseconds, so it
     * is opt-in: a consumer reading a log has to know which it is looking at.
     */
    /**
     * <b>The default.</b> Epoch <b>milliseconds</b>, read through {@link System#nanoTime()}.
     *
     * <p>Fast and monotonic like {@link #nanoEpochClock()}, and — unlike it — in the unit the rest of
     * the framework means by wall-clock time. {@code System.currentTimeMillis()} costs 12.9 ns a call
     * on an Apple M4 against 8.0 for {@code nanoTime}, and an audited path reads the clock once per
     * event, so the anchor-once form is worth having as the default.
     *
     * <p><b>Why milliseconds, when the clock is deliberately unit-free.</b>
     * {@link Clock#getWallClockTime()} returns a bare {@code long} and says nothing about its unit —
     * that is a runtime concern, decided by whichever strategy is installed, which is exactly what
     * makes data-driven replay possible. What the framework does require is that the strategy and the
     * time-based nodes <b>agree</b>, and some of those nodes name their unit in their own API:
     * {@code FixedRateTrigger.atMillis(300)} is milliseconds by construction.
     *
     * <p>The default was briefly {@link #nanoEpochClock()}, on the reasoning that milliseconds cannot
     * express a sub-millisecond duration. True, and beside the point: it left {@code atMillis} callers
     * comparing a millisecond window against a nanosecond clock, so every tumbling and sliding window
     * silently stopped rolling — 30 tests, arithmetic off by a factor of a million, and not one of
     * them mentioning a clock. A default has to agree with the unit the framework's own API names;
     * a graph that installs its own strategy is free to choose any unit, provided its nodes use it.
     */
    static ClockStrategy fastEpochMillisClock() {
        final long epochMillis = System.currentTimeMillis();
        final long nanoBase = System.nanoTime();
        return () -> epochMillis + (System.nanoTime() - nanoBase) / 1_000_000L;
    }

    static ClockStrategy nanoEpochClock() {
        final long epochNanos = System.currentTimeMillis() * 1_000_000L;
        final long nanoBase = System.nanoTime();
        return () -> epochNanos + (System.nanoTime() - nanoBase);
    }

    @Getter
    class ClockStrategyEvent {

        private final ClockStrategy strategy;

        public ClockStrategyEvent(ClockStrategy strategy) {
            this.strategy = strategy;
        }

    }

}
