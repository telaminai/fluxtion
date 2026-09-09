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
import static org.junit.Assert.assertFalse;
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

    /**
     * The same two properties, on {@link BinaryLogRecord}. They are asserted separately because the
     * subclass did not inherit them: it took BOTH times from one method selected by a
     * {@code -Dclock=} system property, a benchmark switch that reached production code. Its default
     * re-introduced the exact defect the base class was fixed for — logTime as a fresh wall-clock
     * reading — and its other mode made endTime a cached value, so every event reported zero duration.
     *
     * <p>Neither showed up in a benchmark. One of them made the binary record look faster than the
     * configuration a user actually gets.
     */
    @Test
    public void theBinaryRecordTakesItsTimesTheSameWayTheTextRecordDoes() {
        SteppingClock strategy = new SteppingClock();
        Clock clock = new Clock();
        clock.init();
        clock.setClockStrategy(new ClockStrategyEvent(strategy));

        BinaryLogRecord record = new BinaryLogRecord(clock, 4096);
        record.updateLogLevel(EventLogControlEvent.LogLevel.INFO);
        record.setRecordEndTime(true);          // off by default: this test is about what it does when on

        strategy.now = 5_000L;
        clock.eventReceived(new Object());      // processing begins: the clock is read once, here
        strategy.now = 9_000L;                  // time moves on before the record is written
        record.triggerObject(new Object());
        record.addRecord(record.internName("node"), record.internName("key"), 1.0);
        strategy.now = 12_500L;                 // work takes 7.5ms in total
        record.terminateRecord();

        assertEquals("logTime must be the reading taken when processing began",
                5_000L, record.logTime());
        assertEquals("endTime must be a live reading so endTime - logTime is the real duration",
                12_500L, record.endTime());
        assertEquals("and the duration must therefore be the real one, not zero",
                7_500L, record.endTime() - record.logTime());
    }

    /**
     * {@code endTime} is <b>off by default</b>. It is the second clock read on an audited event path, and
     * on any event faster than the clock's resolution the duration it exists to report is zero anyway.
     * A deployment that consumes the duration turns it on and pairs it with a clock that can resolve it.
     */
    @Test
    public void endTimeIsNotRecordedUnlessAskedFor() {
        SteppingClock strategy = new SteppingClock();
        Clock clock = new Clock();
        clock.init();
        clock.setClockStrategy(new ClockStrategyEvent(strategy));

        LogRecord text = new LogRecord(clock);
        text.updateLogLevel(EventLogControlEvent.LogLevel.INFO);
        assertFalse("a second clock read per event is opt-in", text.isRecordEndTime());
        strategy.now = 5_000L;
        clock.eventReceived(new Object());
        text.triggerObject(new Object());
        text.addRecord("node", "key", 1);
        strategy.now = 7_500L;
        text.terminateRecord();
        assertFalse("no endTime field when it was never asked for:\n" + text,
                text.toString().contains("endTime"));

        BinaryLogRecord binary = new BinaryLogRecord(clock, 4096);
        binary.updateLogLevel(EventLogControlEvent.LogLevel.INFO);
        assertFalse(binary.isRecordEndTime());
        clock.eventReceived(new Object());
        binary.triggerObject(new Object());
        binary.addRecord(binary.internName("node"), binary.internName("key"), 1.0);
        strategy.now = 9_000L;
        binary.terminateRecord();
        assertEquals("and the binary record leaves the field zero rather than reading a clock",
                0L, binary.endTime());
    }

    @Test
    public void endTimeStaysLiveSoDurationIsNotAlwaysZero() {
        SteppingClock strategy = new SteppingClock();
        Clock clock = new Clock();
        clock.init();
        clock.setClockStrategy(new ClockStrategyEvent(strategy));

        LogRecord record = new LogRecord(clock);
        record.updateLogLevel(EventLogControlEvent.LogLevel.INFO);
        record.setRecordEndTime(true);          // off by default: this test is about what it does when on

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
