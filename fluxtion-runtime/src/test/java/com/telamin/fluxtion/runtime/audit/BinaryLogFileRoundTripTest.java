/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.time.Clock;
import com.telamin.fluxtion.runtime.time.ClockStrategy;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Writer and reader must agree, end to end, through the on-disk framing.
 *
 * <p>The in-memory decoder tests prove the slot layout; these prove the <b>file</b> — the framing, the
 * dictionary, the header, and the damaged-tail behaviour that a real audit log will exercise, because
 * the usual reason to read one is that a process died mid-write.
 */
public class BinaryLogFileRoundTripTest {

    static class Book extends EventLogNode {
        void publish(double price, int qty, boolean live) {
            auditLog.info("price", price).info("qty", qty).info("live", live);
        }
    }

    /** A record with a settable clock, so timestamps in assertions are exact rather than "about now". */
    private static Clock clockAt(long millis) {
        Clock c = new Clock();
        c.init();
        c.setClockStrategy(new ClockStrategy.ClockStrategyEvent(() -> millis));
        return c;
    }

    /** Runs {@code events} through a binary-audited manager and returns the file bytes. */
    private static byte[] writeLog(int events) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        EventLogManager manager = new EventLogManager();
        // One clock, driven exactly as a generated processor drives it: Clock.eventReceived runs
        // BEFORE the audit manager sees the event, which is what populates eventTime and processTime.
        // Skipping that is why the first version of this test read eventTime as 0.
        Clock clock = clockAt(1_000L);
        manager.clock = clock;
        try (BinaryLogWriter writer = new BinaryLogWriter(bytes)) {
            manager.setLogSink(writer);
            manager.init();
            BinaryLogRecord rec = new BinaryLogRecord(clock, 4096);
            rec.setRecordEndTime(true);     // off by default; this test asserts the header carries it
            manager.calculationLogConfig(new EventLogControlEvent(rec));
            Book a = new Book();
            Book b = new Book();
            manager.nodeRegistered(a, "bookA");
            manager.nodeRegistered(b, "bookB");
            for (int i = 0; i < events; i++) {
                Object event = new Object();
                clock.eventReceived(event);
                manager.eventReceived(event);
                a.publish(1.5 + i, 10 + i, i % 2 == 0);
                b.publish(-2.5 - i, 20 + i, i % 2 == 1);
                manager.processingComplete();
            }
        }
        return bytes.toByteArray();
    }

    /** Collects everything the reader reports, in order. */
    static final class Collector implements BinaryLogReader.Visitor {
        final List<String> records = new ArrayList<>();
        final List<String> entries = new ArrayList<>();
        private boolean accept = true;

        Collector() { }

        Collector(boolean accept) { this.accept = accept; }

        @Override
        public boolean onRecord(int typeId, String eventType, long eventTime, long logTime, long endTime, int n) {
            records.add(eventType + "|" + eventTime + "|" + logTime + "|" + endTime + "|" + n);
            return accept;
        }

        @Override
        public void onEntry(int nodeId, String node, int keyId, String key, int tag, long rawBits) {
            entries.add(node + "." + key + "=" + BinaryRecordDecoder.renderValue(tag, rawBits));
        }
    }

    /**
     * Traces survive the file, end to end: written by a logger, encoded as a two-slot entry, framed by
     * the writer, and named by the reader through the dictionary.
     *
     * <p>Until this was fixed a trace went to the byte buffer that {@code length()} does not describe,
     * so it produced no bytes and a trace-only record never published. Every layer here was correct in
     * isolation, which is why only an end-to-end test catches it.
     */
    @Test
    public void tracesSurviveTheRoundTrip() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        EventLogManager manager = new EventLogManager();
        Clock clock = clockAt(1_000L);
        manager.clock = clock;
        try (BinaryLogWriter writer = new BinaryLogWriter(bytes)) {
            manager.setLogSink(writer);
            manager.init();
            manager.calculationLogConfig(
                    new EventLogControlEvent(new BinaryLogRecord(clock, 4096)));
            Book a = new Book();
            manager.nodeRegistered(a, "bookA");
            Object event = new Object();
            clock.eventReceived(event);
            manager.eventReceived(event);
            a.auditLog.info();                 // the node was invoked: a trace, no key, no value
            a.publish(1.5, 10, true);
            manager.processingComplete();
        }
        Collector c = new Collector();
        BinaryLogReader.Result r = BinaryLogReader.read(bytes.toByteArray(), c);

        assertEquals("one record was published", 1, r.records);
        assertTrue("the trace names its node and carries no key or value: " + c.entries,
                c.entries.contains("bookA.null="));
        assertTrue("and the value entries are still there: " + c.entries,
                c.entries.stream().anyMatch(e -> e.startsWith("bookA.") && e.endsWith("=1.5")));
        assertEquals("a trace's absent key is not an id that failed to resolve — that counter is how a "
                + "reader tells a rolled file from a corrupt one", 0, r.unresolvedIds);
    }

    @Test
    public void aWrittenLogReadsBackCompletely() throws IOException {
        byte[] file = writeLog(3);

        Collector c = new Collector();
        BinaryLogReader.Result r = BinaryLogReader.read(file, c);

        assertEquals("three events, three records", 3, r.records);
        assertEquals("6 entries per event", 18, r.entries);
        assertEquals(18, c.entries.size());
        assertEquals("nothing should be unreadable", 0, r.truncatedBytes);
        assertEquals("every id must resolve", 0, r.unresolvedIds);

        assertEquals("bookA.price=1.5", c.entries.get(0));
        assertEquals("bookA.qty=10", c.entries.get(1));
        assertEquals("bookA.live=true", c.entries.get(2));
        assertEquals("bookB.price=-2.5", c.entries.get(3));
        assertEquals("bookB.live=false", c.entries.get(5));
    }

    /** Names are resolved from dictionary frames the writer emitted — not guessed. */
    @Test
    public void theDictionaryTravelsWithTheFile() throws IOException {
        byte[] file = writeLog(1);
        BinaryLogReader.Result r = BinaryLogReader.read(file, new Collector());

        assertTrue("node names must be in the file", r.dictionary.contains("bookA"));
        assertTrue(r.dictionary.contains("bookB"));
        assertTrue("key names too", r.dictionary.contains("price"));
        assertTrue(r.dictionary.contains("qty"));
        assertTrue(r.dictionary.contains("live"));
        assertEquals(0, r.unresolvedIds);
    }

    /** A name is described once, not once per record — otherwise the format is just text again. */
    @Test
    public void dictionaryEntriesAreNotRepeatedPerRecord() throws IOException {
        int oneEvent = writeLog(1).length;
        int tenEvents = writeLog(10).length;

        int perRecord = (tenEvents - oneEvent) / 9;
        // 6 entries x 16 bytes = 96, plus RECORD_FIXED_BYTES (tag + count + typeId + 3 timestamps)
        assertEquals("a record costs its entries plus one fixed frame header",
                6 * 16 + BinaryLogFile.RECORD_FIXED_BYTES, perRecord);
    }

    /** Timestamps survive the round trip — they were silently dropped before the header was exposed. */
    @Test
    public void theRecordHeaderSurvives() throws IOException {
        byte[] file = writeLog(1);
        Collector c = new Collector();
        BinaryLogReader.read(file, c);

        String[] parts = c.records.get(0).split("\\|");
        assertEquals("event type must resolve", "java.lang.Object", parts[0]);
        assertEquals("eventTime", "1000", parts[1]);
        assertEquals("logTime", "1000", parts[2]);
        assertEquals("endTime", "1000", parts[3]);
        assertEquals("entry count", "6", parts[4]);
    }

    /** Declining a record must skip its entries — the cheap path a filter takes. */
    @Test
    public void aVisitorThatDeclinesARecordDoesNotSeeItsEntries() throws IOException {
        byte[] file = writeLog(3);
        Collector c = new Collector(false);
        BinaryLogReader.Result r = BinaryLogReader.read(file, c);

        assertEquals(3, r.records);
        assertEquals("entries are still counted", 18, r.entries);
        assertEquals("but not delivered", 0, c.entries.size());
    }

    /** A process died mid-write. That is the normal case, not an error. */
    @Test
    public void aTruncatedFileReadsWhatItCanAndReportsTheRest() throws IOException {
        byte[] full = writeLog(4);
        byte[] cut = Arrays.copyOf(full, full.length - 40);

        Collector c = new Collector();
        BinaryLogReader.Result r = BinaryLogReader.read(cut, c);

        assertEquals("the whole records before the tear must survive", 3, r.records);
        assertTrue("and the unusable tail must be reported", r.truncatedBytes > 0);
    }

    /** Truncation in the middle of a dictionary frame is equally survivable. */
    @Test
    public void truncationInsideADictionaryFrameIsSurvivable() throws IOException {
        byte[] full = writeLog(1);
        byte[] cut = Arrays.copyOf(full, 12);   // header + part of the first dict frame

        BinaryLogReader.Result r = BinaryLogReader.read(cut, new Collector());
        assertEquals(0, r.records);
        assertTrue(r.truncatedBytes > 0);
    }

    /** An id the dictionary never described renders as #id and is counted, not thrown. */
    @Test
    public void anUnknownIdRendersRatherThanFailing() throws IOException {
        byte[] file = writeLog(1);
        // blank the first dictionary frame's id so nothing resolves it
        file[BinaryLogFile.HEADER_BYTES + 1] = (byte) 0xFF;
        file[BinaryLogFile.HEADER_BYTES + 2] = (byte) 0xFE;

        Collector c = new Collector();
        BinaryLogReader.Result r = BinaryLogReader.read(file, c);

        assertTrue("the read must complete", r.records > 0);
        assertTrue("and say what it could not resolve", r.unresolvedIds > 0);
        assertTrue("rendering an unknown id as #n keeps the record readable",
                c.entries.stream().anyMatch(e -> e.startsWith("#")) || c.records.get(0).startsWith("#"));
    }

    @Test
    public void aFileThatIsNotAnAuditLogIsRejectedClearly() {
        try {
            BinaryLogReader.read("not an audit log at all".getBytes(), new Collector());
            fail("bad magic must be rejected");
        } catch (IOException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("magic"));
        }
    }

    @Test
    public void aFutureFormatVersionIsRejectedRatherThanMisread() throws IOException {
        byte[] file = writeLog(1);
        file[5] = 99;
        try {
            BinaryLogReader.read(file, new Collector());
            fail("an unknown format version must be rejected");
        } catch (IOException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("version"));
        }
    }

    /** The writer must refuse a text record rather than producing an unreadable file. */
    @Test
    public void theWriterRefusesANonBinaryRecord() {
        BinaryLogWriter writer = new BinaryLogWriter(new ByteArrayOutputStream());
        try {
            writer.processLogRecord(new LogRecord(new Clock()));
            fail("a text record must be refused, not silently written");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("BinaryLogRecord"));
        }
    }
}
