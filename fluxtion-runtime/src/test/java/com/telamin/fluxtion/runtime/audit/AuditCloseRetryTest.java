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

    @Test
    public void textWritesAfterAMidCycleExplicitPublicationAreClosedInTheirOwnRecord() {
        List<String> records = new ArrayList<>();
        EventLogManager manager = new EventLogManager(record -> records.add(record.toString())).logLevel(LogLevel.INFO);
        manager.clock = new Time().clock;
        manager.init();
        Source source = new Source();
        manager.nodeRegistered(source, "source");
        manager.eventReceived("DEMO one");
        source.logger.info("value", 1);
        manager.publishLastRecord();
        source.logger.info("after", 2);
        manager.processingComplete();
        manager.eventReceived("DEMO two");
        source.logger.info("value", 3);
        manager.processingComplete();

        assertEquals(3, records.size());
        assertTrue(records.get(1).contains("after: 2}"));
        assertEquals(1, endTimes(records.get(1)));
        String next = records.get(2);
        assertTrue("the next record starts with its own header: " + next, next.startsWith("eventLogRecord: "));
        assertFalse("a late write must not leak into the next record: " + next, next.contains("after"));
        assertEquals(1, endTimes(next));
    }

    @Test
    public void midCycleExplicitPublicationWithNothingAfterItPublishesNoEmptyRecord() {
        for (boolean binary : new boolean[]{false, true}) {
            List<Object> records = new ArrayList<>();
            EventLogManager manager = new EventLogManager(records::add).logLevel(LogLevel.INFO).binaryRecord(binary);
            manager.clock = new Time().clock;
            manager.init();
            Source source = new Source();
            manager.nodeRegistered(source, "source");
            manager.eventReceived("DEMO one");
            source.logger.info("value", 1);
            manager.publishLastRecord();
            manager.processingComplete();
            assertEquals("binary=" + binary, 1, records.size());
        }
    }

    @Test
    public void binaryWritesAfterAMidCycleExplicitPublicationArePublishedNotDropped() throws Exception {
        List<Integer> sizes = new ArrayList<>();
        EventLogManager manager = new EventLogManager(record -> {
            BinaryLogRecord binary = (BinaryLogRecord) record;
            assertTrue("a published record has content", binary.hasContent());
            sizes.add(binary.length());
        }).logLevel(LogLevel.INFO).binaryRecord(true);
        manager.clock = new Time().clock;
        manager.init();
        Source source = new Source();
        manager.nodeRegistered(source, "source");
        manager.eventReceived("DEMO one");
        source.logger.info("value", 1);
        manager.publishLastRecord();
        source.logger.info("after", 2);
        manager.processingComplete();
        manager.eventReceived("DEMO two");
        source.logger.info("value", 3);
        manager.processingComplete();

        assertEquals("the late write is published, not dropped", 3, sizes.size());
        assertEquals("the next record holds only its own entry", sizes.get(0), sizes.get(2));
    }

    /**
     * Review of #53: a mid-cycle {@code publishLastRecord} completed the cycle, so when the close of a
     * late write then threw, the failed-cycle retry took the early return. Text leaked the late entries
     * into the next event's record; binary lost them.
     */
    @Test
    public void lateWriteAfterMidCyclePublicationSurvivesAThrowingCloseInText() {
        List<String> attempts = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("DEMO listener failure");
        EventLogManager manager = new EventLogManager(record -> {
            attempts.add(record.toString());
            if (attempts.size() == 2) throw failure;
        }).logLevel(LogLevel.INFO);
        manager.clock = new Time().clock;
        manager.init();
        Source source = new Source();
        manager.nodeRegistered(source, "source");
        manager.eventReceived("DEMO one");
        source.logger.info("value", 1);
        manager.publishLastRecord();
        source.logger.info("after", 2);

        assertSame(failure, assertThrows(IllegalStateException.class, manager::processingComplete));
        manager.processingComplete();
        assertEquals("the retry publishes the late write", 3, attempts.size());
        assertEquals(attempts.get(1), attempts.get(2));
        assertTrue(attempts.get(2).contains("after: 2}"));

        manager.eventReceived("DEMO two");
        source.logger.info("value", 3);
        manager.processingComplete();
        assertEquals(4, attempts.size());
        String next = attempts.get(3);
        assertTrue("the next record starts with its own header: " + next, next.startsWith("eventLogRecord: "));
        assertFalse("a late write must not leak into the next record: " + next, next.contains("after"));
        assertEquals(1, endTimes(next));
    }

    @Test
    public void lateWriteAfterMidCyclePublicationSurvivesAThrowingCloseInBinary() throws Exception {
        List<Integer> sizes = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("DEMO listener failure");
        EventLogManager manager = new EventLogManager(record -> {
            sizes.add(((BinaryLogRecord) record).length());
            if (sizes.size() == 2) throw failure;
        }).logLevel(LogLevel.INFO).binaryRecord(true);
        manager.clock = new Time().clock;
        manager.init();
        Source source = new Source();
        manager.nodeRegistered(source, "source");
        manager.eventReceived("DEMO one");
        source.logger.info("value", 1);
        manager.publishLastRecord();
        source.logger.info("after", 2);

        assertSame(failure, assertThrows(IllegalStateException.class, manager::processingComplete));
        manager.processingComplete();
        assertEquals("the retry publishes the late write, not drop it", 3, sizes.size());
        assertEquals(sizes.get(1), sizes.get(2));

        manager.eventReceived("DEMO two");
        source.logger.info("value", 3);
        manager.processingComplete();
        assertEquals(4, sizes.size());
        assertEquals("the next record holds only its own entry", sizes.get(0), sizes.get(3));
    }

    /**
     * The same sequence driven the way a generated processor drives it: the normal close throws, and
     * {@code failedCycle} closes the record again. The sink's exception reaches the caller, and the
     * retry inside {@code failedCycle} is the one that publishes.
     */
    @Test
    public void failedCycleRetryPublishesTheLateWriteAndKeepsTheNextRecordClean() {
        for (boolean binary : new boolean[]{false, true}) {
            List<String> published = new ArrayList<>();
            List<Integer> sizes = new ArrayList<>();
            IllegalStateException failure = new IllegalStateException("DEMO listener failure");
            int[] calls = {0};
            EventLogManager manager = new EventLogManager(record -> {
                calls[0]++;
                if (calls[0] == 2) throw failure;
                published.add(binary ? "" : record.toString());
                sizes.add(binary ? ((BinaryLogRecord) record).length() : record.toString().length());
            }).logLevel(LogLevel.INFO).binaryRecord(binary);
            manager.clock = new Time().clock;
            manager.init();
            Source source = new Source();
            manager.nodeRegistered(source, "source");
            GeneratedShapeProcessor processor = new GeneratedShapeProcessor(manager, event -> {
                source.logger.info("value", 1);
                if ("DEMO error handled".equals(event)) {
                    manager.publishLastRecord();
                    source.logger.info("after", 2);
                }
                if ("DEMO node throws".equals(event)) {
                    throw new IllegalArgumentException("DEMO node failure");
                }
            });

            RuntimeException thrown = assertThrows(RuntimeException.class, () -> processor.onEvent("DEMO error handled"));
            assertSame("binary=" + binary, failure, thrown);
            assertEquals("binary=" + binary + ": failedCycle's close succeeded", 0, thrown.getSuppressed().length);
            assertEquals("binary=" + binary + ": explicit record plus the late write", 2, published.size());
            if (!binary) {
                assertTrue(published.get(1).contains("after: 2}"));
            }

            IllegalArgumentException nodeFailure = assertThrows(IllegalArgumentException.class,
                    () -> processor.onEvent("DEMO node throws"));
            assertEquals(0, nodeFailure.getSuppressed().length);
            assertEquals("binary=" + binary + ": failedCycle closed the failed record", 3, published.size());

            processor.onEvent("DEMO plain");
            assertEquals(4, published.size());
            if (binary) {
                assertEquals("records after the failures hold only their own entry", sizes.get(2), sizes.get(3));
            } else {
                String next = published.get(3);
                assertTrue(next, next.startsWith("eventLogRecord: "));
                assertFalse("a late write must not leak into a later record: " + next, next.contains("after"));
                assertEquals(next, 1, next.split("value: 1}", -1).length - 1);
                assertEquals(1, endTimes(next));
            }
        }
    }

    /**
     * Review of #53, finding 2: a close that does not clear the record (clearAfterPublish=false) left
     * it terminated, so a later write was never closed or published.
     */
    @Test
    public void textWriteAfterAnUnclearedCloseIsClosedAndPublished() {
        List<String> records = new ArrayList<>();
        EventLogManager manager = new EventLogManager(record -> records.add(record.toString())).logLevel(LogLevel.INFO);
        manager.clock = new Time().clock;
        manager.init();
        manager.setClearAfterPublish(false);
        Source source = new Source();
        manager.nodeRegistered(source, "source");
        manager.eventReceived("DEMO one");
        source.logger.info("value", 1);
        manager.processingComplete();
        source.logger.info("after", 2);
        manager.processingComplete();

        assertEquals("the late write is published", 2, records.size());
        assertTrue("the published bytes are kept, the late write follows them", records.get(1).startsWith(records.get(0)));
        assertTrue("the late write's node map is closed: " + records.get(1), records.get(1).contains("after: 2}"));
        manager.processingComplete();
        assertEquals("nothing new, nothing published", 2, records.size());
    }

    @Test
    public void binaryWriteAfterAnUnclearedCloseIsClosedAndPublished() throws Exception {
        Time time = new Time();
        List<long[]> records = new ArrayList<>();
        EventLogManager manager = new EventLogManager(record -> {
            BinaryLogRecord binary = (BinaryLogRecord) record;
            records.add(new long[]{binary.length(), binary.endTime()});
        }).logLevel(LogLevel.INFO).binaryRecord(true);
        manager.clock = time.clock;
        manager.init();
        manager.setClearAfterPublish(false);
        Source source = new Source();
        manager.nodeRegistered(source, "source");
        manager.eventReceived("DEMO one");
        source.logger.info("value", 1);
        manager.processingComplete();
        time.now = 2000;
        source.logger.info("after", 2);
        manager.processingComplete();

        assertEquals("the late write is published", 2, records.size());
        assertTrue("the late entry is in the record", records.get(1)[0] > records.get(0)[0]);
        assertEquals("the reopened record is closed again", 2000, records.get(1)[1]);
    }

    /**
     * Finding 2, second shape: publishLastRecord's sink throws, so nothing clears the record, and a node
     * then writes. The cycle's own close must close that write, not return the earlier verdict.
     */
    @Test
    public void writeAfterAThrowingExplicitPublicationIsClosedByTheCycle() {
        for (boolean binary : new boolean[]{false, true}) {
            Time time = new Time();
            List<String> texts = new ArrayList<>();
            List<Long> endTimes = new ArrayList<>();
            IllegalStateException failure = new IllegalStateException("DEMO listener failure");
            EventLogManager manager = new EventLogManager(record -> {
                texts.add(binary ? "" : record.toString());
                endTimes.add(binary ? ((BinaryLogRecord) record).endTime() : 0L);
                if (texts.size() == 1) throw failure;
            }).logLevel(LogLevel.INFO).binaryRecord(binary);
            manager.clock = time.clock;
            manager.init();
            Source source = new Source();
            manager.nodeRegistered(source, "source");
            manager.eventReceived("DEMO one");
            source.logger.info("value", 1);
            assertSame(failure, assertThrows(IllegalStateException.class, manager::publishLastRecord));
            time.now = 2000;
            source.logger.info("after", 2);
            manager.processingComplete();

            assertEquals("binary=" + binary, 2, texts.size());
            if (binary) {
                assertEquals("the cycle's close takes a new endTime", 2000L, endTimes.get(1).longValue());
            } else {
                assertTrue("the late write's node map is closed: " + texts.get(1), texts.get(1).contains("after: 2}"));
                assertEquals("the cycle's close adds its own endTime", 2, endTimes(texts.get(1)));
            }
            manager.eventReceived("DEMO two");
            source.logger.info("value", 3);
            manager.processingComplete();
            assertEquals("binary=" + binary, 3, texts.size());
            if (!binary) {
                assertFalse(texts.get(2).contains("after"));
            }
        }
    }

    @Test
    public void quotedScalarRecordRetriesWithOneMarker() {
        List<String> attempts = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("DEMO listener failure");
        EventLogManager manager = new EventLogManager(record -> {
            attempts.add(record.toString());
            if (attempts.size() == 1) throw failure;
        }).logLevel(LogLevel.INFO);
        manager.clock = new Time().clock;
        manager.init();
        Source source = new Source();
        manager.nodeRegistered(source, "source");
        manager.eventReceived("DEMO one");
        source.logger.info("text", "DEMO\nline");
        assertSame(failure, assertThrows(IllegalStateException.class, manager::processingComplete));
        manager.processingComplete();

        assertEquals(2, attempts.size());
        assertEquals("a retry re-sends the same bytes", attempts.get(0), attempts.get(1));
        assertEquals(attempts.get(1), 1, markers(attempts.get(1)));
        assertTrue(attempts.get(1).startsWith("eventLogRecord: " + LogRecord.TEXT_ENCODING_MARKER));

        manager.eventReceived("DEMO two");
        source.logger.info("value", 3);
        manager.processingComplete();
        assertEquals(3, attempts.size());
        assertEquals("a plain record after a quoted one has no marker", 0, markers(attempts.get(2)));
    }

    @Test
    public void quotedLateWriteAfterAnUnclearedCloseKeepsOneMarker() {
        List<String> records = new ArrayList<>();
        EventLogManager manager = new EventLogManager(record -> records.add(record.toString())).logLevel(LogLevel.INFO);
        manager.clock = new Time().clock;
        manager.init();
        manager.setClearAfterPublish(false);
        Source source = new Source();
        manager.nodeRegistered(source, "source");
        manager.eventReceived("DEMO one");
        source.logger.info("text", "DEMO\nline");
        manager.processingComplete();
        source.logger.info("more", "DEMO\nmore");
        manager.processingComplete();

        assertEquals(2, records.size());
        assertEquals(records.get(1), 1, markers(records.get(1)));
        assertTrue(records.get(1).contains("more: \"DEMO\\nmore\"}"));
    }

    // ---------------------------------------------------------------------------------------------
    // Review of #53 (2026-11-09), F1: a second explicit publication that throws stays retryable.
    // ---------------------------------------------------------------------------------------------

    @Test
    public void secondThrowingExplicitPublicationIsRetriedByCleanupInText() throws Exception {
        assertSecondExplicitPublicationRetried(false);
    }

    @Test
    public void secondThrowingExplicitPublicationIsRetriedByCleanupInBinary() throws Exception {
        assertSecondExplicitPublicationRetried(true);
    }

    @Test
    public void throwingExplicitPublicationAfterARetainedCloseIsRetriedInText() throws Exception {
        assertRetainedExplicitPublicationRetried(false);
    }

    @Test
    public void throwingExplicitPublicationAfterARetainedCloseIsRetriedInBinary() throws Exception {
        assertRetainedExplicitPublicationRetried(true);
    }

    /** The generated close shape: a node publishes twice, the second publication throws, failedCycle retries. */
    @Test
    public void failedCycleRetriesASecondThrowingExplicitPublication() throws Exception {
        for (boolean binary : new boolean[]{false, true}) {
            List<String> attempts = new ArrayList<>();
            IllegalStateException failure = new IllegalStateException("DEMO listener failure");
            EventLogManager manager = new EventLogManager(record -> {
                attempts.add(capture(record));
                if (attempts.size() == 2) throw failure;
            }).logLevel(LogLevel.INFO).binaryRecord(binary);
            manager.clock = new Time().clock;
            manager.init();
            Source source = new Source();
            manager.nodeRegistered(source, "source");
            GeneratedShapeProcessor processor = new GeneratedShapeProcessor(manager, event -> {
                source.logger.info("value", 1);
                if ("DEMO publish twice".equals(event)) {
                    manager.publishLastRecord();
                    source.logger.info("late", 2);
                    manager.publishLastRecord();
                }
            });

            RuntimeException thrown = assertThrows(RuntimeException.class, () -> processor.onEvent("DEMO publish twice"));
            assertSame("binary=" + binary + ": the sink's exception reaches the caller", failure, thrown);
            assertEquals("binary=" + binary + ": failedCycle's retry succeeded", 0, thrown.getSuppressed().length);
            assertEquals("binary=" + binary + ": failedCycle retried the second publication", 3, attempts.size());
            assertEquals("binary=" + binary + ": the retry re-sends the same bytes", attempts.get(1), attempts.get(2));

            processor.onEvent("DEMO plain");
            assertEquals(4, attempts.size());
            assertCleanNext(binary, attempts.get(0), attempts.get(3), "late");
        }
    }

    private static void assertSecondExplicitPublicationRetried(boolean binary) throws Exception {
        List<String> attempts = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("DEMO listener failure");
        Time time = new Time();
        EventLogManager manager = new EventLogManager(record -> {
            attempts.add(capture(record));
            if (attempts.size() == 2) throw failure;
        }).logLevel(LogLevel.INFO).binaryRecord(binary);
        manager.clock = time.clock;
        manager.init();
        Source source = new Source();
        manager.nodeRegistered(source, "source");
        manager.eventReceived("DEMO one");
        source.logger.info("value", 1);
        manager.publishLastRecord();
        source.logger.info("late", 2);

        assertSame("the sink's exception reaches the caller", failure,
                assertThrows(IllegalStateException.class, manager::publishLastRecord));
        time.now = 2000;
        manager.processingComplete();
        assertEquals("failed-cycle cleanup retries the throwing second publication", 3, attempts.size());
        assertEquals("the retry re-sends the same closed bytes", attempts.get(1), attempts.get(2));
        if (!binary) {
            assertTrue(attempts.get(2), attempts.get(2).contains("late: 2}"));
            assertEquals(1, endTimes(attempts.get(2)));
        }
        manager.processingComplete();
        assertEquals("cleanup after the successful retry publishes nothing", 3, attempts.size());

        manager.eventReceived("DEMO two");
        source.logger.info("value", 3);
        manager.processingComplete();
        assertEquals(4, attempts.size());
        assertCleanNext(binary, attempts.get(0), attempts.get(3), "late");
    }

    private static void assertRetainedExplicitPublicationRetried(boolean binary) throws Exception {
        List<String> attempts = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("DEMO listener failure");
        Time time = new Time();
        EventLogManager manager = new EventLogManager(record -> {
            attempts.add(capture(record));
            if (attempts.size() == 2) throw failure;
        }).logLevel(LogLevel.INFO).binaryRecord(binary);
        manager.clock = time.clock;
        manager.init();
        manager.setClearAfterPublish(false);
        Source source = new Source();
        manager.nodeRegistered(source, "source");
        manager.eventReceived("DEMO one");
        source.logger.info("value", 1);
        manager.processingComplete();
        source.logger.info("late", 2);

        assertSame("the sink's exception reaches the caller", failure,
                assertThrows(IllegalStateException.class, manager::publishLastRecord));
        time.now = 2000;
        manager.processingComplete();
        assertEquals("failed-cycle cleanup retries the throwing explicit publication", 3, attempts.size());
        assertEquals("the retry re-sends the same closed bytes", attempts.get(1), attempts.get(2));
        manager.processingComplete();
        assertEquals("cleanup after the successful retry publishes nothing", 3, attempts.size());

        manager.eventReceived("DEMO two");
        source.logger.info("value", 3);
        manager.processingComplete();
        assertEquals(4, attempts.size());
        if (binary) {
            assertEquals("the next binary record holds only its own entry",
                    attempts.get(0).length(), attempts.get(3).length());
        } else {
            // publishLastRecord always clears, so the retained buffer starts again at the next event.
            assertCleanNext(false, attempts.get(0), attempts.get(3), "late");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Review of #53 (2026-11-09), F2: adopting the same text record keeps its completed close.
    // ---------------------------------------------------------------------------------------------

    @Test
    public void adoptingAClosedTextRecordKeepsTheFailedPublicationRetryable() {
        for (boolean trace : new boolean[]{false, true}) {
            for (boolean logValue : new boolean[]{false, true}) {
                if (!trace && !logValue) continue; // nothing to publish: no business entry, no trace
                List<String> attempts = new ArrayList<>();
                IllegalStateException failure = new IllegalStateException("DEMO listener failure");
                Time time = new Time();
                EventLogManager manager = new EventLogManager(record -> {
                    attempts.add(record.toString());
                    if (attempts.size() == 1) throw failure;
                }).logLevel(LogLevel.INFO);
                if (trace) manager.tracingOn(LogLevel.INFO);
                manager.clock = time.clock;
                manager.init();
                Source source = new Source();
                manager.nodeRegistered(source, "source");
                String label = "trace=" + trace + " logValue=" + logValue + ": ";
                manager.eventReceived("DEMO one");
                if (logValue) source.logger.info("value", 1);
                assertSame(failure, assertThrows(IllegalStateException.class, manager::processingComplete));

                manager.calculationLogConfig(new EventLogControlEvent(new LogRecord(time.clock)));
                time.now = 2000;
                manager.processingComplete();
                assertEquals(label + "the adopted record's failed publication is retried", 2, attempts.size());
                assertEquals(label + "the retry re-sends the closed bytes", attempts.get(0), attempts.get(1));
                assertEquals(label, 1, endTimes(attempts.get(1)));
                manager.processingComplete();
                assertEquals(label + "nothing more after a successful retry", 2, attempts.size());

                manager.eventReceived("DEMO two");
                source.logger.info("value", 3);
                manager.processingComplete();
                assertEquals(label, 3, attempts.size());
                String next = attempts.get(2);
                assertTrue(label + next, next.startsWith("eventLogRecord: "));
                assertFalse(label + next, next.contains("DEMO one"));
                assertEquals(label, 1, endTimes(next));
            }
        }
    }

    @Test
    public void adoptingAnOpenTextRecordClosesItOnceWithAThrowingSink() {
        for (boolean trace : new boolean[]{false, true}) {
            List<String> attempts = new ArrayList<>();
            IllegalStateException failure = new IllegalStateException("DEMO listener failure");
            Time time = new Time();
            EventLogManager manager = new EventLogManager(record -> {
                attempts.add(record.toString());
                if (attempts.size() == 1) throw failure;
            }).logLevel(LogLevel.INFO);
            if (trace) manager.tracingOn(LogLevel.INFO);
            manager.clock = time.clock;
            manager.init();
            Source source = new Source();
            manager.nodeRegistered(source, "source");
            manager.eventReceived("DEMO one");
            source.logger.info("value", 1);
            manager.calculationLogConfig(new EventLogControlEvent(new LogRecord(time.clock)));
            source.logger.info("after", 2);
            assertSame(failure, assertThrows(IllegalStateException.class, manager::processingComplete));
            time.now = 2000;
            manager.processingComplete();
            assertEquals("trace=" + trace, 2, attempts.size());
            assertEquals("trace=" + trace, attempts.get(0), attempts.get(1));
            String record = attempts.get(1);
            assertTrue(record, record.contains("value: 1, after: 2}"));
            assertEquals(record, 1, endTimes(record));
            assertTrue(record, record.contains("endTime: 1000"));
        }
    }

    @Test
    public void adoptionCarriesTheCompletedCloseAndReplacementResetsIt() {
        for (boolean logValue : new boolean[]{false, true}) {
            Time time = new Time();
            LogRecord first = new LogRecord(time.clock);
            first.triggerObject("DEMO event");
            if (logValue) first.addRecord("source", "value", 1);
            assertEquals(logValue, first.terminateRecord());
            String closed = first.toString();

            LogRecord second = new LogRecord(time.clock);
            second.replaceBuffer(first.asCharSequence());
            second.adoptRecordState(first);
            time.now = 2000;
            assertEquals("adoption keeps the close verdict", logValue, second.terminateRecord());
            assertEquals("adoption keeps the closed bytes", closed, second.toString());

            // Intentionally replacing the content starts a new close.
            second.replaceBuffer(closed);
            second.terminateRecord();
            assertEquals("replacement resets the close", 2, endTimes(second.toString()));
        }

        // An open record adopted mid-record is still open: one close, at the adopter.
        Time time = new Time();
        LogRecord open = new LogRecord(time.clock);
        open.triggerObject("DEMO event");
        open.addRecord("source", "value", 1);
        LogRecord adopter = new LogRecord(time.clock);
        adopter.replaceBuffer(open.asCharSequence());
        adopter.adoptRecordState(open);
        adopter.addRecord("source", "after", 2);
        assertTrue(adopter.terminateRecord());
        assertTrue(adopter.toString(), adopter.toString().contains("value: 1, after: 2}"));
        assertEquals(1, endTimes(adopter.toString()));
    }

    /** Text as written; binary as its encoded bytes, hex. */
    private static String capture(LogRecord record) {
        if (!(record instanceof BinaryLogRecord)) {
            return record.toString();
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            record.encodeTo(out);
            StringBuilder hex = new StringBuilder();
            for (byte b : out.toByteArray()) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }

    /** The next record is clean: its own header and entry only (text), or one entry's size (binary). */
    private static void assertCleanNext(boolean binary, String firstRecord, String next, String lateKey) {
        if (binary) {
            assertEquals("the next binary record holds only its own entry", firstRecord.length(), next.length());
        } else {
            assertTrue("the next record starts with its own header: " + next, next.startsWith("eventLogRecord: "));
            assertFalse("a late write must not leak into the next record: " + next, next.contains(lateKey));
            assertEquals(next, 1, endTimes(next));
        }
    }

    private static int markers(String text) {
        return text.split(LogRecord.TEXT_ENCODING_MARKER, -1).length - 1;
    }

    /** The close sequence a generated processor emits: {@code afterEvent}, and {@code failedCycle} on a throw. */
    private static final class GeneratedShapeProcessor {
        private final EventLogManager auditor;
        private final java.util.function.Consumer<Object> node;

        private GeneratedShapeProcessor(EventLogManager auditor, java.util.function.Consumer<Object> node) {
            this.auditor = auditor;
            this.node = node;
        }

        void onEvent(Object event) {
            auditor.eventReceived(event);
            try {
                node.accept(event);
                auditor.processingComplete();
            } catch (Throwable t) {
                failedCycle(t);
                throw t;
            }
        }

        private void failedCycle(Throwable cause) {
            try {
                auditor.processingComplete();
            } catch (Throwable t) {
                if (t != cause) {
                    cause.addSuppressed(t);
                }
            }
        }
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
