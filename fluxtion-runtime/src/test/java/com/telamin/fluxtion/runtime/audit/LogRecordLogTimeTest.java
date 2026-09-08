/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.time.Clock;
import com.telamin.fluxtion.runtime.time.ClockStrategy;
import com.telamin.fluxtion.runtime.time.ClockStrategy.ClockStrategyEvent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@code logTime} must be the time processing began — the reading {@link Clock#eventReceived} already
 * took — and not a second, later reading of the wall clock.
 *
 * <p>The old behaviour could not be caught by a test that only checked "logTime is a plausible time":
 * a later reading is also plausible. Driving the clock explicitly makes the difference observable.
 */
public class LogRecordLogTimeTest {

    /** A clock the test moves by hand, so "the reading taken at eventReceived" is a distinct value. */
    static final class SteppingClock implements ClockStrategy {
        long now = 1_000L;

        @Override
        public long getWallClockTime() {
            return now;
        }
    }

    @Test
    public void logTimeIsTheReadingTakenWhenProcessingBegan() {
        SteppingClock strategy = new SteppingClock();
        Clock clock = new Clock();
        clock.init();
        clock.setClockStrategy(new ClockStrategyEvent(strategy));

        LogRecord record = new LogRecord(clock);
        record.updateLogLevel(EventLogControlEvent.LogLevel.INFO);

        strategy.now = 5_000L;
        clock.eventReceived(new Object());      // processing begins: the clock is read once, here
        strategy.now = 9_000L;                  // time moves on before the record is written
        record.triggerObject(new Object());

        String yaml = record.toString();
        assertTrue("logTime must be the reading taken when processing began, was:\n" + yaml,
                yaml.contains("logTime: 5000"));
        assertEquals(5_000L, clock.getProcessTime());
    }

    @Test
    public void endTimeStaysLiveSoDurationIsNotAlwaysZero() {
        SteppingClock strategy = new SteppingClock();
        Clock clock = new Clock();
        clock.init();
        clock.setClockStrategy(new ClockStrategyEvent(strategy));

        LogRecord record = new LogRecord(clock);
        record.updateLogLevel(EventLogControlEvent.LogLevel.INFO);

        strategy.now = 5_000L;
        clock.eventReceived(new Object());
        record.triggerObject(new Object());
        record.addRecord("node", "key", 1);
        strategy.now = 7_500L;                  // work takes 2.5ms
        record.terminateRecord();

        String yaml = record.toString();
        assertTrue(yaml, yaml.contains("logTime: 5000"));
        assertTrue("endTime must be a live reading so endTime - logTime is the real duration, was:\n" + yaml,
                yaml.contains("endTime: 7500"));
    }
}
