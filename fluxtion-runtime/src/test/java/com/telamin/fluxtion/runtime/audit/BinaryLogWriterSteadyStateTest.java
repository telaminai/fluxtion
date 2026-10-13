/*
 * Copyright: © 2026. Gregory Higgins
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.time.Clock;
import com.telamin.fluxtion.runtime.time.ClockStrategy;
import org.junit.Assume;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * What the audit FILE sink costs per record once it is running, and that making it cheaper did not
 * move a byte of the file.
 *
 * <p>Two defects, measured on a released runtime. The writer called {@code BinaryLogRecord.dictionary()}
 * on every record - a new {@code String[]} and a walk of the id map, 152 B/record, enough to exhaust a
 * 64 MB Epsilon heap - under a comment saying the common case cost nothing. And it wrote every long as
 * eight {@code write(int)} calls, about 109 per record, so an unbuffered file stream was a system call
 * per byte. The writer now checks the dictionary's size before reading it and encodes each record into
 * a reused array written with one call.
 *
 * <p>Each property has a witness: restore the old {@code translationFor} and the allocation tests fail;
 * restore byte-at-a-time writes and the write-count test fails; change any encoding and the
 * byte-identity tests fail against {@link ByteAtATimeReferenceWriter}, a frozen copy of the old writer.
 */
public class BinaryLogWriterSteadyStateTest {

    private static final int RECORDS = 20_000;

    /** Discards bytes; counts calls, so a test can tell a bulk write from a byte-at-a-time one. */
    static final class CountingSink extends OutputStream {
        long bytes;
        long writeCalls;

        @Override
        public void write(int b) {
            bytes++;
            writeCalls++;
        }

        @Override
        public void write(byte[] b, int off, int len) {
            bytes += len;
            writeCalls++;
        }
    }

    static final class Tick { }

    /** Both writers fed from the SAME published record, so any difference is the writer's. */
    static final class Tee implements LogRecordListener {
        final LogRecordListener[] sinks;

        Tee(LogRecordListener... sinks) {
            this.sinks = sinks;
        }

        @Override
        public void processLogRecord(LogRecord logRecord) {
            for (LogRecordListener sink : sinks) {
                sink.processLogRecord(logRecord);
            }
        }
    }

    private static Clock clock() {
        Clock c = new Clock();
        c.init();
        final long[] now = {1_700_000_000_000L};
        c.setClockStrategy(new ClockStrategy.ClockStrategyEvent(() -> now[0]++));
        return c;
    }

    /** One event of four entries, published to {@code sink}. Allocates nothing itself. */
    private static void publish(BinaryLogRecord rec, Object event, int node, int[] keys, int i,
                                LogRecordListener sink) {
        rec.triggerObject(event);
        rec.addRecord(node, keys[0], 1.5 + i);
        rec.addRecord(node, keys[1], (long) i);
        rec.addRecord(node, keys[2], i);
        rec.addRecord(node, keys[3], (i & 1) == 0);
        rec.terminateRecord();
        sink.processLogRecord(rec);
        rec.clear();
    }

    private static com.sun.management.ThreadMXBean threadBean() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        Assume.assumeTrue("needs per-thread allocation accounting",
                bean instanceof com.sun.management.ThreadMXBean
                        && ((com.sun.management.ThreadMXBean) bean).isThreadAllocatedMemorySupported());
        com.sun.management.ThreadMXBean sun = (com.sun.management.ThreadMXBean) bean;
        sun.setThreadAllocatedMemoryEnabled(true);
        return sun;
    }

    private static long allocated(com.sun.management.ThreadMXBean bean) {
        return bean.getThreadAllocatedBytes(Thread.currentThread().getId());
    }

    /**
     * Bytes allocated per record over {@link #RECORDS} steady-state records. The measurement brackets
     * the loop, not each record, so the accounting call's own cost is spread over 20,000 records; the
     * old writer measured 152 B/record here, three orders of magnitude above the bound.
     */
    private static double bytesPerRecord(com.sun.management.ThreadMXBean bean, BinaryLogRecord rec,
                                         Object event, int node, int[] keys, LogRecordListener sink) {
        long before = allocated(bean);
        for (int i = 0; i < RECORDS; i++) {
            publish(rec, event, node, keys, i, sink);
        }
        return (allocated(bean) - before) / (double) RECORDS;
    }

    @Test
    public void steadyStateRecordsAllocateNothing() {
        com.sun.management.ThreadMXBean bean = threadBean();
        BinaryLogRecord rec = new BinaryLogRecord(clock());
        int node = rec.internName("bookA");
        int[] keys = {rec.internName("price"), rec.internName("qty"), rec.internName("size"),
                rec.internName("live")};
        BinaryLogWriter writer = new BinaryLogWriter(new CountingSink());
        Tick event = new Tick();
        bytesPerRecord(bean, rec, event, node, keys, writer);   // warm-up: first record emits DICT frames

        double perRecord = bytesPerRecord(bean, rec, event, node, keys, writer);
        assertTrue("steady-state records must allocate nothing; measured " + perRecord + " B/record",
                perRecord < 1.0);
    }

    /**
     * The dictionary GROWS mid-stream - a new node and a String value interned partway through. The
     * record that introduces them may allocate (DICT frames, a new translation table); every record
     * after it must not, because the cache is keyed on the size it grew to, not the size it started at.
     */
    @Test
    public void afterTheDictionaryGrowsTheSteadyStateAllocatesNothingAgain() {
        com.sun.management.ThreadMXBean bean = threadBean();
        BinaryLogRecord rec = new BinaryLogRecord(clock());
        int node = rec.internName("bookA");
        int[] keys = {rec.internName("price"), rec.internName("qty"), rec.internName("size"),
                rec.internName("live")};
        CountingSink sink = new CountingSink();
        BinaryLogWriter writer = new BinaryLogWriter(sink);
        Tick event = new Tick();
        bytesPerRecord(bean, rec, event, node, keys, writer);

        // growth: a new node, and a CharSequence value - which is a dictionary id too
        int sizeBefore = rec.dictionarySize();
        long dictFramesBefore = sink.writeCalls;
        int late = rec.internName("bookLate");
        rec.triggerObject(event);
        rec.addRecord("bookLate", "state", (CharSequence) "OPEN");
        rec.terminateRecord();
        writer.processLogRecord(rec);
        rec.clear();
        assertEquals("the growth record really grew the dictionary", sizeBefore + 3, rec.dictionarySize());
        assertEquals("three DICT frames (head + name each) and the record", 3 * 2 + 1,
                sink.writeCalls - dictFramesBefore);

        double perRecord = bytesPerRecord(bean, rec, event, late, keys, writer);
        assertTrue("records after dictionary growth must allocate nothing; measured " + perRecord
                + " B/record", perRecord < 1.0);
    }

    /** A record larger than any before grows the encode buffer once; the same size after it is free. */
    @Test
    public void aLargerRecordGrowsTheBufferOnceAndThenAllocatesNothing() {
        com.sun.management.ThreadMXBean bean = threadBean();
        BinaryLogRecord rec = new BinaryLogRecord(clock());
        int node = rec.internName("wide");
        int key = rec.internName("v");
        BinaryLogWriter writer = new BinaryLogWriter(new CountingSink());
        Tick event = new Tick();
        int entries = 300;                       // past the initial 64-entry buffer
        Runnable wide = () -> {
            rec.triggerObject(event);
            for (int e = 0; e < entries; e++) {
                rec.addRecord(node, key, (long) e);
            }
            rec.terminateRecord();
            writer.processLogRecord(rec);
            rec.clear();
        };
        for (int i = 0; i < 100; i++) {
            wide.run();
        }
        long before = allocated(bean);
        for (int i = 0; i < 2_000; i++) {
            wide.run();
        }
        double perRecord = (allocated(bean) - before) / 2_000.0;
        assertTrue("a 300-entry steady state must allocate nothing; measured " + perRecord + " B/record",
                perRecord < 1.0);
    }

    /**
     * One {@code write} call per steady-state record, whatever its size. The old writer made
     * 1 + 2 + 2 + 3*8 + entries*16 single-byte calls - 93 for four entries - and its unbuffered
     * documented form was a system call for each.
     */
    @Test
    public void aSteadyStateRecordIsOneWriteCall() {
        BinaryLogRecord rec = new BinaryLogRecord(clock());
        int node = rec.internName("bookA");
        int[] keys = {rec.internName("price"), rec.internName("qty"), rec.internName("size"),
                rec.internName("live")};
        CountingSink sink = new CountingSink();
        BinaryLogWriter writer = new BinaryLogWriter(sink);
        assertEquals("the header is one write", 1, sink.writeCalls);
        Tick event = new Tick();
        publish(rec, event, node, keys, 0, writer);   // first record: DICT frames + the record

        long calls = sink.writeCalls;
        long bytes = sink.bytes;
        for (int i = 0; i < 1_000; i++) {
            publish(rec, event, node, keys, i, writer);
        }
        assertEquals("one write per record", 1_000, sink.writeCalls - calls);
        assertEquals("and every byte of it", 1_000L * (BinaryLogFile.RECORD_FIXED_BYTES + 4 * 16),
                sink.bytes - bytes);
    }

    // ---------------------------------------------------------------- byte identity

    /**
     * The bulk writer's file equals the byte-at-a-time writer's, record for record, through every path
     * the writer has: dictionary growth mid-stream, CharSequence and Object values (dictionary ids),
     * null values, an empty record, a record past the initial buffer, multi-byte UTF-8 names, and a
     * record instance REPLACED mid-stream with a different id assignment (the translation path).
     */
    @Test
    public void bulkWriterBytesEqualTheByteAtATimeWriter() throws IOException {
        for (int unit : new int[]{BinaryLogFile.TIME_UNIT_EPOCH_MILLIS, BinaryLogFile.TIME_UNIT_EPOCH_NANOS,
                BinaryLogFile.TIME_UNIT_UNSPECIFIED}) {
            ByteArrayOutputStream bulk = new ByteArrayOutputStream();
            ByteArrayOutputStream reference = new ByteArrayOutputStream();
            BinaryLogWriter bulkWriter = new BinaryLogWriter(bulk, unit);
            ByteAtATimeReferenceWriter referenceWriter = new ByteAtATimeReferenceWriter(reference, unit);
            Tee both = new Tee(bulkWriter, referenceWriter);
            writeEveryPath(both);
            byte[] expected = reference.toByteArray();
            byte[] actual = bulk.toByteArray();
            assertTrue("the scenario wrote a real file", expected.length > 10_000);
            assertArrayEquals("unit " + unit + ": first difference at byte " + firstDifference(expected, actual),
                    expected, actual);
            assertEquals(referenceWriter.recordsWritten(), bulkWriter.recordsWritten());
        }
    }

    /** And the file reads back: every record, every entry, every name, nothing unresolved. */
    @Test
    public void theBulkWritersFileReadsBackWhole() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BinaryLogWriter writer = new BinaryLogWriter(bytes);
        long entries = writeEveryPath(writer);
        final StringBuilder names = new StringBuilder();
        BinaryLogReader.Result result = BinaryLogReader.read(bytes.toByteArray(), new BinaryLogReader.Visitor() {
            @Override
            public boolean onRecord(int eventTypeId, String eventType, long eventTime, long logTime,
                                    long endTime, int entryCount) {
                return true;
            }

            @Override
            public void onEntry(int nodeId, String node, int keyId, String key, int tag, long rawBits) {
                names.append(node).append('.').append(key).append(' ');
            }
        });
        assertEquals(writer.recordsWritten(), result.records);
        assertEquals(entries, result.entries);
        assertEquals("no torn tail", 0, result.truncatedBytes);
        assertEquals("every id resolved", 0, result.unresolvedIds);
        assertEquals(0, result.redefinedIds);
        assertTrue(names.toString(), names.indexOf("bookLate.state") >= 0);
        assertTrue(names.toString(), names.indexOf("böök€😀.price") >= 0);
        assertTrue("the replacement record's entries kept their names", names.indexOf("swapNode.k") >= 0);
    }

    /** Drives every writer path; returns the number of entries written. */
    private static long writeEveryPath(LogRecordListener sink) {
        Clock clock = clock();
        long entries = 0;
        BinaryLogRecord rec = new BinaryLogRecord(clock);
        int node = rec.internName("bookA");
        int[] keys = {rec.internName("price"), rec.internName("qty"), rec.internName("size"),
                rec.internName("live")};
        Tick tick = new Tick();
        for (int i = 0; i < 50; i++) {
            publish(rec, tick, node, keys, i, sink);
            entries += 4;
        }
        // growth mid-stream: a new node, a multi-byte name, String and Object values, nulls
        String multiByte = "böök€😀";
        for (int i = 0; i < 50; i++) {
            rec.triggerObject(i % 2 == 0 ? tick : "a String event");
            rec.addRecord("bookLate", "state", (CharSequence) ("S" + (i % 7)));
            rec.addRecord(multiByte, "price", -0.0 - i);
            rec.addRecord("bookLate", "obj", (Object) Arrays.asList(i % 3, "x"));
            rec.addRecord("bookLate", "none", (CharSequence) null);
            rec.addRecord("bookLate", null, 'c');
            rec.addTrace("bookLate");
            rec.terminateRecord();
            sink.processLogRecord(rec);
            rec.clear();
            entries += 6;
        }
        // an empty record
        rec.triggerObject(tick);
        rec.terminateRecord();
        sink.processLogRecord(rec);
        rec.clear();
        // a record past the initial encode buffer
        rec.triggerObject(tick);
        for (int e = 0; e < 300; e++) {
            rec.addRecord(node, keys[e & 3], Long.MIN_VALUE + e);
        }
        rec.terminateRecord();
        sink.processLogRecord(rec);
        rec.clear();
        entries += 300;
        // the record instance REPLACED, with ids assigned in a different order - the translation path
        BinaryLogRecord swap = new BinaryLogRecord(clock);
        swap.internName("swapNode");
        swap.internName("k");
        int swapNode = swap.internName("bookA");     // a name the file already has, at a new id
        int swapKey = swap.internName("price");
        for (int i = 0; i < 20; i++) {
            swap.triggerObject(tick);
            swap.addRecord(swapNode, swapKey, (double) i);
            swap.addRecord("swapNode", "k", i);
            swap.terminateRecord();
            sink.processLogRecord(swap);
            swap.clear();
            entries += 2;
        }
        // and back to the first record, whose cached translation is now stale
        for (int i = 0; i < 10; i++) {
            publish(rec, tick, node, keys, i, sink);
            entries += 4;
        }
        return entries;
    }

    private static int firstDifference(byte[] a, byte[] b) {
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            if (a[i] != b[i]) {
                return i;
            }
        }
        return a.length == b.length ? -1 : n;
    }
}
