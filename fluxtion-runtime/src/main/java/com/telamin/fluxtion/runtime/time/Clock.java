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
     * The default strategy is {@link ClockStrategy#nanoEpochClock()} — <b>nanoseconds</b> since the
     * epoch, derived from {@code System.nanoTime()} and anchored once.
     *
     * <p>It was {@code System::currentTimeMillis}, which is both slower and unable to represent what it
     * is read for. Measured on an Apple M4: {@code currentTimeMillis} costs 12.9 ns a call against 8.0
     * for {@code nanoTime}, and it advances a thousand times a second — so on any event faster than a
     * millisecond, a duration taken across two readings is always exactly zero. An audited path reads
     * the clock once here per event.
     *
     * <p><b>The unit changed with this.</b> {@link #getWallClockTime()} now returns nanoseconds where it
     * returned milliseconds. Call {@link #setClockStrategy} with {@code () -> System.currentTimeMillis()}
     * to restore the old behaviour, or supply a data-driven strategy for replay.
     */
    @Initialise
    @Override
    public void init() {
        wallClock = ClockStrategy.nanoEpochClock();
    }

}
