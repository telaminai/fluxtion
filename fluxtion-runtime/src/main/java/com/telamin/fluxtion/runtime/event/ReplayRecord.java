/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
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
