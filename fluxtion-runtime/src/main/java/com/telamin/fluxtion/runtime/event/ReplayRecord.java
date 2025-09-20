/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.event;

import com.telamin.fluxtion.runtime.CloneableDataFlow;

/**
 * Holds an event with a wall clock time, ready to be replayed into an {@link CloneableDataFlow}.
 * <p>
 * See YamlReplayRunner in Fluxtion compiler for an example of replaying ReplayRecords into an {@link CloneableDataFlow}
 * <p>
 * See YamlReplayRecordWriter in Fluxtion compiler for an example of recording ReplayRecords from an {@link CloneableDataFlow}
 */
public class ReplayRecord {
    private long wallClockTime;
    private Object event;

    public long getWallClockTime() {
        return wallClockTime;
    }

    public void setWallClockTime(long wallClockTime) {
        this.wallClockTime = wallClockTime;
    }

    public Object getEvent() {
        return event;
    }

    public void setEvent(Object event) {
        this.event = event;
    }
}
