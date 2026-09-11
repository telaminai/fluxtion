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
    private transient boolean shareReading = false;
    public static final Clock DEFAULT_CLOCK = new Clock();

    /**
     * While set, {@code eventReceived} REUSES the current reading instead of taking a new one.
     *
     * <p>For re-entrant cycles. Every element a flatMap emits is dispatched as its own graph cycle
     * through the normal event path, so a three-element flatMap read the clock FOUR times for one
     * arrival — one for the event and one per element. The elements did not arrive at different times;
     * they are consequences of one arrival in one wave, so sharing its instant is more faithful as well
     * as cheaper. The generated processor sets this around the queued-callback drain and restores it.
     *
     * <p>Measured in the C++ target, which does the same around its callback cycle: a reading costs
     * 6.7 ns there, so a three-element flatMap was spending about 27 ns of clock per source event.
     *
     * <p><b>Restore it.</b> Left set, every later event carries this one's timestamp — worse than the
     * cost it saves.
     *
     * @return the previous setting, so a caller can restore it
     */
    public boolean shareReading(boolean share) {
        boolean previous = shareReading;
        shareReading = share;
        return previous;
    }

    /**
     * An {@link Event} supplies its OWN event time. {@link Event#getEventTime()} is the producer's
     * statement of when the event happened - by contract epoch milliseconds at construction, or -1
     * for none - so it is recorded as given, in the producer's unit, while {@code processTime} is a
     * reading of the installed {@link ClockStrategy}. Under a non-millisecond strategy the two differ
     * in unit; the audit file's header unit describes the strategy's readings, not this value. See
     * {@code BinaryLogFile.TIME_UNIT_EPOCH_MILLIS}.
     */
    @Override
    public void eventReceived(Event event) {
        if (!shareReading) {
            processTime = getWallClockTime();
        }
        eventTime = event.getEventTime();
    }

    @Override
    public void eventReceived(Object event) {
        if (!shareReading) {
            processTime = getWallClockTime();
        }
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
     * Current wall-clock time from the installed {@link ClockStrategy}. <b>Milliseconds</b> since the
     * epoch under the default strategy; whatever unit a supplied strategy uses otherwise.
     *
     * @return current time from the clock strategy
     */
    public long getWallClockTime() {
        return wallClock.getWallClockTime();
    }

    /**
     * The default strategy is {@code System::currentTimeMillis} — <b>milliseconds</b> since the epoch,
     * read fresh on every call.
     *
     * <p><b>Why a CURRENT reading and not a faster projected one.</b>
     * {@link ClockStrategy#fastEpochMillisClock()} is cheaper — {@code currentTimeMillis} costs 12.9 ns
     * a call on an Apple M4 against 8.0 for {@code nanoTime} — but it anchors once and then advances
     * from {@code nanoTime}, so it never sees a later NTP or manual wall-clock correction. This class
     * promises the CURRENT time, and the runtime itself is an absolute-time consumer: {@link
     * #eventReceived} stores this reading as {@code processTime} and the audit record emits it as
     * {@code logTime}. A long-lived process on the projected clock keeps stamping records on a
     * pre-correction timeline. That is a different contract, so it is opt-in rather than the default.
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
     * <p>Both alternatives are opt-in through {@link #setClockStrategy}:
     * {@link ClockStrategy#fastEpochMillisClock()} for cost, if you accept that it will not track a
     * wall-clock correction; {@link ClockStrategy#nanoEpochClock()} for sub-millisecond timestamps, if
     * the graph has no time-windowed nodes; or a data-driven strategy for replay.
     */
    @Initialise
    @Override
    public void init() {
        wallClock = System::currentTimeMillis;
    }

}
