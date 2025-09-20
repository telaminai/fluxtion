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
import com.telamin.fluxtion.runtime.annotations.NoTriggerReference;
import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import lombok.Getter;

//@EqualsAndHashCode
public class FixedRateTrigger {

    @NoTriggerReference
    private final Clock clock;
    private final int rate;
    private long previousTime;
    /**
     * number of triggers that should have fired since the last time update to now
     */
    @Getter
    private int triggerCount;

    public static FixedRateTrigger atMillis(int millis) {
        return new FixedRateTrigger(millis);
    }

    public FixedRateTrigger(int rate) {
        this(Clock.DEFAULT_CLOCK, rate);
    }

    public FixedRateTrigger(Clock clock, int rate) {
        this.clock = clock;
        this.rate = rate;
    }

    @OnEventHandler
    public boolean hasExpired(Object input) {
        long newTime = clock.getWallClockTime();
        boolean expired = rate <= (newTime - previousTime);
        if (expired) {
            triggerCount = (int) ((newTime - previousTime) / rate);
            previousTime += (long) triggerCount * rate;
        }
        return expired;
    }

    @OnEventHandler(propagate = false)
    public boolean setClockStrategy(ClockStrategy.ClockStrategyEvent event) {
        init();
        return false;
    }

    @Initialise
    public void init() {
        previousTime = clock.getWallClockTime();
        triggerCount = 0;
    }
}
