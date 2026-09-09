/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.time;

import com.telamin.fluxtion.runtime.annotations.Initialise;
import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import com.telamin.fluxtion.runtime.audit.Auditor;
import com.telamin.fluxtion.runtime.event.Event;
import com.telamin.fluxtion.runtime.time.ClockStrategy.ClockStrategyEvent;

/**
 * A clock instance in a static event processor, use the @Inject annotation to
 * ensure the same of instance of the clock is used for all nodes. Clock
 * provides time query functionality for the processor as follows:
 *
 * <ul>
 * <li>WallClock - current time UTC milliseconds</li>
 * <li>ProcessTime - the time the event was received for processing</li>
 * <li>EventTime - the time the event was created</li>
 * </ul>
 *
 * @author 2024 gregory higgins.
 */
public class Clock implements Auditor, Auditor.FirstAfterEvent {

    private transient long eventTime;
    private transient long processTime;
    private ClockStrategy wallClock;
    public static final Clock DEFAULT_CLOCK = new Clock();

    @Override
    public void eventReceived(Event event) {
        processTime = getWallClockTime();
        eventTime = event.getEventTime();
    }

    @Override
    public void eventReceived(Object event) {
        processTime = getWallClockTime();
        eventTime = processTime;
    }

    @Override
    public void nodeRegistered(Object node, String nodeName) {/*NoOp*/
    }

    @OnEventHandler(propagate = false)
    public void setClockStrategy(ClockStrategyEvent event) {
        this.wallClock = event.getStrategy();
    }

    /**
     * The time the last event was received by the processor
     *
     * @return time the last event was received for processing
     */
    public long getProcessTime() {
        return processTime;
    }

    /**
     * The time the latest event was created
     *
     * @return time the latest event was created
     */
    public long getEventTime() {
        return eventTime;
    }

    /**
     * Current wall-clock time from the installed {@link ClockStrategy}. Nanoseconds since the epoch
     * under the default strategy; whatever unit a supplied strategy uses otherwise.
     *
     * @return current time from the clock strategy
     */
    public long getWallClockTime() {
        return wallClock.getWallClockTime();
    }

    /**
     * The default strategy is {@link ClockStrategy#fastEpochMillisClock()} — <b>milliseconds</b> since
     * the epoch, derived from {@code System.nanoTime()} and anchored once.
     *
     * <p>It was {@code System::currentTimeMillis}, which is slower for no benefit: measured on an
     * Apple M4, {@code currentTimeMillis} costs 12.9 ns a call against 8.0 for {@code nanoTime}, and an
     * audited path reads the clock once here per event. The default is now anchored once and read
     * through {@code nanoTime} — same unit, less cost.
     *
     * <p><b>{@link #getWallClockTime()} is deliberately unit-free</b> — a bare {@code long}, with the
     * unit a runtime concern belonging to whichever {@link ClockStrategy} is installed. That is what
     * lets a replay drive the graph from recorded data. The requirement is only that the strategy and
     * the graph's time-based nodes agree.
     *
     * <p>The DEFAULT, though, has to agree with the unit the framework's own API names, and
     * {@code FixedRateTrigger.atMillis()} names milliseconds. This default was briefly
     * {@link ClockStrategy#nanoEpochClock()}; every tumbling and sliding window stopped rolling,
     * thirty tests failed, and not one of them mentioned a clock. Recorded rather than quietly
     * reverted, because the failure was silent and a long way from its cause.
     *
     * <p>Sub-millisecond precision is still available and still opt-in: call {@link #setClockStrategy}
     * with {@link ClockStrategy#nanoEpochClock()} if a graph wants nanosecond timestamps and has no
     * time-windowed nodes, or supply a data-driven strategy for replay.
     */
    @Initialise
    @Override
    public void init() {
        wallClock = ClockStrategy.fastEpochMillisClock();
    }

}
