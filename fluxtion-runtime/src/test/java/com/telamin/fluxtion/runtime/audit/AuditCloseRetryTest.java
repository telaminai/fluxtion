/*
 * Copyright: © 2026. Gregory Higgins <gregphiggins@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;
import com.telamin.fluxtion.runtime.event.DefaultEvent;
import com.telamin.fluxtion.runtime.time.Clock;
import com.telamin.fluxtion.runtime.time.ClockStrategy.ClockStrategyEvent;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class AuditCloseRetryTest {

    @Test
    public void throwingListenerCanRetryTheSameClosedRecord() {
        for (boolean trace : new boolean[]{false, true}) {
            for (boolean clear : new boolean[]{false, true}) {
                for (boolean endTime : new boolean[]{false, true}) {
                    assertListenerRetry(trace, clear, endTime, false, true);
                }
            }
        }
        // A trace-only record has no business fields, but still needs a stable close on retry.
        assertListenerRetry(true, true, true, false, false);
    }

    @Test
    public void explicitErrorPublicationCanRetryTheSameClosedRecord() {
        assertListenerRetry(false, false, true, true, true);
    }

    @Test
    public void explicitPublicationRemainsAvailableAfterSuccessfulCompletion() {
        List<String> records = new ArrayList<>();
        EventLogManager manager = new EventLogManager(record -> records.add(record.toString())).logLevel(LogLevel.INFO);
        manager.clock = new Time().clock;
        manager.init();
        manager.setClearAfterPublish(false);
        Source source = new Source();
        manager.nodeRegistered(source, "source");
        manager.eventReceived("DEMO event");
        source.logger.info("value", 7);
        manager.processingComplete();
        manager.publishLastRecord();
        assertEquals(2, records.size());
        assertEquals("explicit publication keeps the completed record intact", records.get(0), records.get(1));
        manager.processingComplete();
        assertEquals("explicit publication also completed the cycle", 2, records.size());
    }

    private static void assertListenerRetry(boolean trace, boolean clear, boolean endTime,
                                           boolean explicitPublication, boolean logValues) {
        List<String> attempts = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("DEMO listener failure");
        Time time = new Time();
        EventLogManager manager = new EventLogManager(record -> {
            attempts.add(record.toString());
            if (attempts.size() == 1) throw failure;
        }).logLevel(LogLevel.INFO).recordEndTime(endTime);
        if (trace) manager.tracingOn(LogLevel.INFO);
        manager.clock = time.clock;
        manager.init();
        manager.setClearAfterPublish(clear);
        Source source = new Source();
        manager.nodeRegistered(source, "source");
        manager.eventReceived("DEMO event");
        if (logValues) source.logger.info("value", 7);
        Runnable close = explicitPublication ? manager::publishLastRecord : manager::processingComplete;

        assertSame(failure, assertThrows(IllegalStateException.class, close::run));
        assertEquals(endTime ? 1 : 0, endTimes(attempts.get(0)));
        time.now = 2000;
        close.run();
        assertEquals("retry must reach the sink even without tracing", 2, attempts.size());
        assertEquals("a retry must not append an endTime or sample a new one", attempts.get(0), attempts.get(1));
        assertEquals(endTime ? 1 : 0, endTimes(attempts.get(1)));
        assertEquals(clear || explicitPublication ? "" : attempts.get(1), manager.lastRecordAsString());
        manager.processingComplete();
        assertEquals("cleanup after successful publication must not publish an orphan record", 2, attempts.size());

        manager.eventReceived(new DefaultEvent() { });
        if (logValues) source.logger.info("value", 8);
        manager.processingComplete();
        assertEquals("the next typed event can publish", 3, attempts.size());
        manager.eventReceived("DEMO next object");
        if (logValues) source.logger.info("value", 9);
        manager.processingComplete();
        assertEquals("the next object event can publish", 4, attempts.size());
    }

    @Test
    public void textCloseVerdictAndBytesStayStableUntilTheNextRecord() {
        Time time = new Time();
        LogRecord record = new LogRecord(time.clock);
        record.triggerObject("DEMO first");
        record.addRecord("source", "value", 7);
        assertTrue(record.terminateRecord());
        String closed = record.toString();
        time.now = 2000;
        assertTrue("keep the publish decision for a retry", record.terminateRecord());
        assertEquals(closed, record.toString());
        assertEquals(1, endTimes(closed));

        // Retaining the buffer does not retain the previous event's close verdict.
        record.triggerEvent(new DefaultEvent() { });
        assertFalse(record.terminateRecord());
        assertEquals(2, endTimes(record.toString()));
        record.clear();
        record.triggerObject("DEMO fresh");
        assertFalse(record.terminateRecord());
        assertEquals(1, endTimes(record.toString()));
        assertTrue(record.toString().contains("endTime: 2000"));
    }

    @Test
    public void replacingOrDiscardingTheTextBufferResetsCloseState() {
        Time time = new Time();
        LogRecord record = new LogRecord(time.clock);
        record.triggerObject("DEMO first");
        record.terminateRecord();
        record.replaceBuffer("eventLogRecord: \n    nodeLogs: ");
        time.now = 2000;
        record.terminateRecord();
        assertEquals(1, endTimes(record.toString()));
        assertTrue(record.toString().contains("endTime: 2000"));

        record.updateLogLevel(LogLevel.NONE);
        record.terminateRecord();
        assertEquals("", record.toString());
        record.updateLogLevel(LogLevel.INFO);
        record.triggerObject("DEMO enabled");
        record.terminateRecord();
        assertEquals(1, endTimes(record.toString()));
    }

    @Test
    public void binaryCloseKeepsItsTimestampAndEncodedBytesAndResetsOnReuse() throws Exception {
        for (boolean endTime : new boolean[]{false, true}) {
            Time time = new Time();
            BinaryLogRecord record = new BinaryLogRecord(time.clock, 4096);
            record.setRecordEndTime(endTime);
            record.triggerObject("DEMO first");
            record.addRecord(record.internName("source"), record.internName("value"), 7L);
            assertTrue(record.terminateRecord());
            ByteArrayOutputStream first = new ByteArrayOutputStream();
            record.encodeTo(first);
            time.now = 2000;
            assertTrue(record.terminateRecord());
            ByteArrayOutputStream retry = new ByteArrayOutputStream();
            record.encodeTo(retry);
            assertArrayEquals(first.toByteArray(), retry.toByteArray());
            assertEquals(endTime ? 1000 : 0, record.endTime());

            record.triggerEvent(new DefaultEvent() { });
            assertFalse(record.terminateRecord());
            assertEquals(endTime ? 2000 : 0, record.endTime());
            record.clear();
            time.now = 3000;
            record.addRecord(record.internName("source"), record.internName("value"), 8L);
            assertTrue(record.terminateRecord());
            assertEquals(endTime ? 3000 : 0, record.endTime());
        }
    }

    private static int endTimes(String text) {
        return text.split("\\n    endTime:", -1).length - 1;
    }

    private static final class Source implements EventLogSource {
        private EventLogger logger;
        @Override public void setLogger(EventLogger logger) { this.logger = logger; }
    }

    private static final class Time {
        private long now = 1000;
        private final Clock clock = new Clock();
        private Time() {
            clock.init();
            clock.setClockStrategy(new ClockStrategyEvent(() -> now));
        }
    }
}
