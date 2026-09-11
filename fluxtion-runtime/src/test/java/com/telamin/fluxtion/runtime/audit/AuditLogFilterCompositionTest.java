package com.telamin.fluxtion.runtime.audit;

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
}
