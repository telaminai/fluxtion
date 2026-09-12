package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.audit.BinaryLogFile;
import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;
import com.telamin.fluxtion.runtime.audit.tools.AuditLogFilter;
import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * The CLI filter's options composed, and its diagnostics honest about name ROLES.
 *
 * <p>Both defects were invisible to the existing tool test because its fixture put every node in every
 * record and exercised {@code --limit} without an entry filter — so neither interaction could arise.
 */
public class AuditLogFilterCompositionTest {

    /** Two records: the requested node appears only in the SECOND. */
    private Path twoRecordsTargetInSecond() throws Exception {
        Path file = Files.createTempFile("filter-compose", ".flxa");
        Clock clock = new Clock();
        clock.init();
        // ONE record, cleared and reused - what EventLogManager does. A record per iteration restarts
        // the dictionary ids and the writer would refuse it.
        BinaryLogRecord r = new BinaryLogRecord(clock, 4096);
        r.updateLogLevel(LogLevel.INFO);
        try (BinaryLogWriter w = new BinaryLogWriter(Files.newOutputStream(file))) {
            for (String node : new String[]{"other", "target"}) {
                r.clear();
                r.triggerObject(new Object());
                EventLogger logger = new BinaryEventLogger(r, node);
                logger.setLevel(LogLevel.INFO);
                logger.info("k", 1);
                r.terminateRecord();
                w.processLogRecord(r);
            }
        }
        return file;
    }

    private static final class Capture implements AuditLogFilter.Sink {
        final List<String> records = new ArrayList<>();
        final List<String> entries = new ArrayList<>();
        public void record(String eventType, long eventTime, long logTime, long endTime) {
            records.add(String.valueOf(eventType));
        }
        public void entry(String node, String key, String value) { entries.add(node + "." + key); }
    }

    /**
     * A record with no matching entry must not consume {@code --limit}.
     *
     * <p>The header used to be emitted and counted before entry filters ran, so the first
     * non-matching record spent the limit and the matching one was never reached.
     */
    @Test
    public void nodeFilterAndLimitCompose() throws Exception {
        Capture out = new Capture();
        AuditLogFilter filter = new AuditLogFilter(
                null, "target", null, Long.MIN_VALUE, Long.MAX_VALUE, 1, out);
        BinaryLogReader.read(twoRecordsTargetInSecond(), filter);

        assertEquals("the one matching entry must be found despite --limit 1",
                1, out.entries.size());
        assertEquals("and it must be the target node", "target.k", out.entries.get(0));
        assertEquals("exactly one record should be reported", 1, out.records.size());
    }

    /**
     * An {@code --event} pattern that only matches a NODE name is unmatchable as an event pattern, and
     * the tool should say so rather than reporting an empty result.
     */
    @Test
    public void aPatternMatchingOnlyAnotherRoleIsReportedUnmatchable() throws Exception {
        Capture out = new Capture();
        AuditLogFilter filter = new AuditLogFilter(
                "target", null, null, Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, out);
        BinaryLogReader.read(twoRecordsTargetInSecond(), filter);

        assertEquals("no record has that EVENT type", 0, out.records.size());
        assertEquals("the diagnostic must name the unmatchable pattern rather than staying silent",
                "--event target", filter.unmatchableWithinSelection());
    }

    /** A pattern that does match in its own role must NOT be reported unmatchable. */
    @Test
    public void aPatternMatchingInItsOwnRoleIsNotReportedUnmatchable() throws Exception {
        Capture out = new Capture();
        AuditLogFilter filter = new AuditLogFilter(
                null, "target", null, Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, out);
        BinaryLogReader.read(twoRecordsTargetInSecond(), filter);

        assertEquals("the node filter matched", 1, out.entries.size());
        assertNull("nothing is unmatchable here", filter.unmatchableWithinSelection());
    }

    // ---- round 7, B5: a redefinition re-evaluates every role's match bit ----

    private static void dict(java.io.ByteArrayOutputStream out, int id, String name) {
        byte[] b = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        out.write(BinaryLogFile.FRAME_DICT);
        out.write(id >>> 8); out.write(id);
        out.write(b.length >>> 8); out.write(b.length);
        out.write(b, 0, b.length);
    }

    private static void record(java.io.ByteArrayOutputStream out, int type, int node, int key, long value) {
        out.write(BinaryLogFile.FRAME_RECORD);
        out.write(0); out.write(1);              // one entry
        out.write(type >>> 8); out.write(type);
        for (long t : new long[]{1L, 1L, 2L}) { for (int sh = 56; sh >= 0; sh -= 8) out.write((int) (t >>> sh)); }
        long slot0 = ((long) node << 48) | ((long) key << 32) | 2L;   // TAG_LONG
        for (int sh = 56; sh >= 0; sh -= 8) out.write((int) (slot0 >>> sh));
        for (int sh = 56; sh >= 0; sh -= 8) out.write((int) (value >>> sh));
    }

    /** One header; id 2 is a node named oldNode, then newNode, then oldNode again; a record after each. */
    private static byte[] redefinitions() throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        new BinaryLogWriter(out).close();       // header only
        dict(out, 1, "Tick"); dict(out, 2, "oldNode"); dict(out, 3, "price");
        record(out, 1, 2, 3, 42);
        dict(out, 2, "newNode");
        record(out, 1, 2, 3, 43);
        dict(out, 2, "oldNode");
        record(out, 1, 2, 3, 44);
        return out.toByteArray();
    }

    private static List<String> selected(String eventGlob, String nodeGlob, String keyGlob, byte[] file) throws Exception {
        List<String> out = new ArrayList<>();
        AuditLogFilter f = new AuditLogFilter(eventGlob, nodeGlob, keyGlob, (Long) null, null, Long.MAX_VALUE,
                new AuditLogFilter.Sink() {
                    public void record(String t, long a, long b, long c) { }
                    public void entry(String node, String key, String value) { out.add(node + "." + key + "=" + value); }
                });
        BinaryLogReader.read(file, f);
        return out;
    }

    /**
     * REVIEWER PROBE (round 7). The filter only ever SET a match bit, so an id whose name stopped
     * matching kept matching: a query for oldNode selected the record written under newNode. The bit
     * is now the answer for the id's CURRENT name - matching, non-matching, matching again.
     */
    @Test
    public void aRedefinedNameDoesNotKeepItsOldMatch() throws Exception {
        byte[] file = redefinitions();
        assertEquals(List.of("oldNode.price=42", "oldNode.price=44"), selected(null, "oldNode", null, file));
        assertEquals(List.of("newNode.price=43"), selected(null, "newNode", null, file));
        assertEquals("unfiltered: all three, each under its then-current name",
                List.of("oldNode.price=42", "newNode.price=43", "oldNode.price=44"), selected(null, null, null, file));
    }

    @Test
    public void redefinitionReEvaluatesKeyAndEventBitsToo() throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        new BinaryLogWriter(out).close();
        dict(out, 1, "Tick"); dict(out, 2, "n"); dict(out, 3, "price");
        record(out, 1, 2, 3, 1);
        dict(out, 3, "size");                    // the KEY id renamed
        record(out, 1, 2, 3, 2);
        dict(out, 1, "Other");                   // the EVENT id renamed
        record(out, 1, 2, 3, 3);
        byte[] file = out.toByteArray();
        assertEquals(List.of("n.price=1"), selected(null, null, "price", file));
        assertEquals(List.of("n.size=2", "n.size=3"), selected(null, null, "size", file));
        assertEquals(List.of("n.price=1", "n.size=2"), selected("Tick", null, null, file));
        assertEquals(List.of("n.size=3"), selected("Other", null, null, file));
    }
}
