/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
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
     * Current wallclock time in milliseconds UTC
     *
     * @return time in milliseconds UTC
     */
    public long getWallClockTime() {
        return wallClock.getWallClockTime();
    }

    @Initialise
    @Override
    public void init() {
        wallClock = System::currentTimeMillis;
    }

}
