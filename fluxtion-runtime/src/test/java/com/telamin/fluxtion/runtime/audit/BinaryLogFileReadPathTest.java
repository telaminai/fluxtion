/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.time.Clock;
import com.telamin.fluxtion.runtime.time.ClockStrategy;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The two ways a log file is read: <b>memory-mapped</b> when it fits one mapping, and <b>streamed</b>
 * when it does not. Both must produce identical results, and the streamed path must handle a frame
 * that straddles a chunk boundary.
 *
 * <p>The straddle case is forced with a deliberately tiny chunk rather than a 2 GiB fixture, because a
 * fixture that large would never be built and the case would go untested — which is exactly how a
 * record spanning a boundary comes to be reported as corruption in a perfectly good file.
 */
public class BinaryLogFileReadPathTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    static class Book extends EventLogNode {
        void publish(double price, int qty) {
            auditLog.info("price", price).info("qty", qty);
        }
    }

    private static byte[] buildLog(int events) throws IOException {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        writeInto(bytes, events);
        return bytes.toByteArray();
    }

    private static void writeInto(OutputStream out, int events) throws IOException {
        EventLogManager manager = new EventLogManager();
        Clock clock = new Clock();
        clock.init();
        clock.setClockStrategy(new ClockStrategy.ClockStrategyEvent(() -> 4_242L));
        manager.clock = clock;
        try (BinaryLogWriter writer = new BinaryLogWriter(out)) {
            manager.setLogSink(writer);
            manager.init();
            manager.calculationLogConfig(new EventLogControlEvent(new BinaryLogRecord(clock, 4096)));
            Book a = new Book();
            manager.nodeRegistered(a, "bookA");
            for (int i = 0; i < events; i++) {
                Object e = new Object();
                clock.eventReceived(e);
                manager.eventReceived(e);
                a.publish(1.0 + i, i);
                manager.processingComplete();
            }
        }
    }

    static final class Collector implements BinaryLogReader.Visitor {
        final List<String> entries = new ArrayList<>();
        int records;

        @Override
        public boolean onRecord(String t, long ev, long lg, long en, int n) {
            records++;
            return true;
        }

        @Override
        public void onEntry(String node, String key, int tag, long bits) {
            entries.add(node + "." + key + "=" + BinaryRecordDecoder.renderValue(tag, bits));
        }
    }

    @Test
    public void readingFromAFileMatchesReadingFromBytes() throws IOException {
        byte[] bytes = buildLog(5);
        Path file = folder.newFile("audit.flxa").toPath();
        Files.write(file, bytes);

        Collector fromBytes = new Collector();
        BinaryLogReader.Result rb = BinaryLogReader.read(bytes, fromBytes);

        Collector fromFile = new Collector();
        BinaryLogReader.Result rf = BinaryLogReader.read(file, fromFile);

        assertEquals(rb.records, rf.records);
        assertEquals(rb.entries, rf.entries);
        assertEquals(0, rf.truncatedBytes);
        assertEquals(0, rf.unresolvedIds);
        assertEquals(fromBytes.entries, fromFile.entries);
        assertEquals("5 events x 2 values", 10, fromFile.entries.size());
    }

    /**
     * The straddle case the spec requires: a chunk small enough that records certainly span boundaries.
     * A record here is 2 entries — 32 bytes of slots plus a 29-byte frame header — so a 7-byte chunk
     * cuts through every frame repeatedly.
     */
    @Test
    public void aFrameStraddlingAChunkBoundaryIsCarriedNotLost() throws IOException {
        byte[] bytes = buildLog(6);

        Collector whole = new Collector();
        BinaryLogReader.read(bytes, whole);

        for (int chunk : new int[]{7, 13, 16, 31, 61, 64}) {
            Collector streamed = new Collector();
            BinaryLogReader.Result r = BinaryLogReader.readStreamed(
                    new ByteArrayInputStream(bytes), chunk, streamed);

            assertEquals("chunk " + chunk + ": every record must survive",
                    whole.records, streamed.records);
            assertEquals("chunk " + chunk + ": every entry must survive",
                    whole.entries, streamed.entries);
            assertEquals("chunk " + chunk + ": a complete file has no truncation",
                    0, r.truncatedBytes);
            assertEquals("chunk " + chunk + ": every id must still resolve",
                    0, r.unresolvedIds);
        }
    }

    /** A chunk smaller than a single frame must grow the buffer, not stall or corrupt. */
    @Test
    public void aChunkSmallerThanOneFrameStillCompletes() throws IOException {
        byte[] bytes = buildLog(3);
        Collector streamed = new Collector();
        BinaryLogReader.Result r = BinaryLogReader.readStreamed(
                new ByteArrayInputStream(bytes), 1, streamed);

        assertEquals(3, streamed.records);
        assertEquals(6, streamed.entries.size());
        assertEquals(0, r.truncatedBytes);
    }

    /** Truncation is still reported on the streamed path, and only for the genuinely partial tail. */
    @Test
    public void streamedReadingStillReportsATruncatedTail() throws IOException {
        byte[] full = buildLog(4);
        byte[] cut = java.util.Arrays.copyOf(full, full.length - 20);

        Collector streamed = new Collector();
        BinaryLogReader.Result r = BinaryLogReader.readStreamed(
                new ByteArrayInputStream(cut), 16, streamed);

        assertEquals("the whole records before the tear survive", 3, streamed.records);
        assertTrue("and the partial tail is reported", r.truncatedBytes > 0);
    }

    /** An empty file is not an audit log and must say so rather than returning nothing. */
    @Test
    public void anEmptyFileIsRejected() throws IOException {
        Path file = folder.newFile("empty.flxa").toPath();
        try {
            BinaryLogReader.read(file, new Collector());
            org.junit.Assert.fail("an empty file is not an audit log");
        } catch (IOException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("not an audit log"));
        }
    }

    /** The mapping threshold is the MappedByteBuffer limit, not a guess. */
    @Test
    public void theSingleMapThresholdIsTheMappedByteBufferLimit() {
        assertEquals("a single mapping cannot exceed Buffer's int capacity",
                Integer.MAX_VALUE, BinaryLogReader.MAX_SINGLE_MAP);
    }

    /**
     * <b>The two read paths must agree.</b> Found by mutation: forcing every file down the streamed
     * path made {@code anEmptyFileIsRejected} fail, because the streamed path returned an empty result
     * where the mapped path threw. "Read a file" must not mean something different depending on how
     * big the file is.
     */
    @Test
    public void bothReadPathsRejectAFileWithNoValidHeader() {
        byte[][] rubbish = {
                new byte[0],
                "hi".getBytes(),
                "not an audit log at all".getBytes(),
        };
        for (byte[] bad : rubbish) {
            String mapped = null;
            String streamed = null;
            try {
                BinaryLogReader.read(bad, new Collector());
            } catch (IOException e) {
                mapped = e.getMessage();
            }
            try {
                BinaryLogReader.readStreamed(new ByteArrayInputStream(bad), 4, new Collector());
            } catch (IOException e) {
                streamed = e.getMessage();
            }
            assertTrue("mapped path must reject " + bad.length + " bytes of rubbish", mapped != null);
            assertTrue("streamed path must reject it too", streamed != null);
        }
    }

    /** And a stream that ends mid-header is rejected, not treated as an empty log. */
    @Test
    public void aStreamEndingInsideTheHeaderIsRejected() throws IOException {
        byte[] full = buildLog(1);
        byte[] halfHeader = java.util.Arrays.copyOf(full, 5);
        try {
            BinaryLogReader.readStreamed(new ByteArrayInputStream(halfHeader), 2, new Collector());
            org.junit.Assert.fail("half a header is not an audit log");
        } catch (IOException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("not an audit log"));
        }
    }
}
