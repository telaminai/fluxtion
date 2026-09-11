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
     * <b>Opt-in.</b> Epoch <b>milliseconds</b>, projected from {@link System#nanoTime()}.
     *
     * <p>Anchors {@code System.currentTimeMillis()} against {@code System.nanoTime()} ONCE and advances
     * by the monotonic delta thereafter. Cheaper than the default — {@code currentTimeMillis} costs
     * 12.9 ns a call on an Apple M4 against 8.0 for {@code nanoTime}, and an audited path reads the
     * clock once per event — and monotonic, so it will not step backwards over an NTP correction.
     *
     * <p><b>It also never steps FORWARD over one.</b> The wall clock is sampled once, at construction,
     * and never again: this reports elapsed time projected from that anchor, not the current time. A
     * host clock correction — NTP, an operator, a VM resume — is invisible to it, and a long-lived
     * process goes on stamping a pre-correction timeline. Drift accumulates and is never reconciled.
     *
     * <p>That is why this is not the default. It is the right choice when the readings are used for
     * DURATIONS, and the wrong one when they are used as absolute timestamps — and the runtime's own
     * audit record is the second case: {@code Clock.eventReceived} stores the reading and the record
     * emits it as {@code logTime}. Choose it deliberately, for a process whose lifetime and accuracy
     * needs you know.
     *
     * @return a monotonic epoch-millisecond strategy that does not track wall-clock corrections
     */
    static ClockStrategy fastEpochMillisClock() {
        final long epochMillis = System.currentTimeMillis();
        final long nanoBase = System.nanoTime();
        return () -> epochMillis + (System.nanoTime() - nanoBase) / 1_000_000L;
    }

    /**
     * <b>Opt-in.</b> Epoch <b>nanoseconds</b>, projected from {@link System#nanoTime()}.
     *
     * <p>Same anchor-once behaviour as {@link #fastEpochMillisClock()} and the same caveat: it does not
     * track later wall-clock corrections.
     *
     * <p><b>The unit differs from the default</b>, which is milliseconds, so a consumer reading a log
     * has to know which produced it — and the framework's own time-windowed nodes name milliseconds
     * ({@code FixedRateTrigger.atMillis}). Installing this on a graph with a tumbling or sliding window
     * stops the window rolling, silently. Use it when sub-millisecond timestamps matter and the graph
     * has no time-windowed nodes.
     *
     * @return a monotonic epoch-nanosecond strategy that does not track wall-clock corrections
     */
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
