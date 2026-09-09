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
