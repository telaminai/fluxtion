/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit.tools;

import com.telamin.fluxtion.runtime.audit.BinaryLogRecord;
import com.telamin.fluxtion.runtime.audit.BinaryLogWriter;
import com.telamin.fluxtion.runtime.audit.EventLogControlEvent;
import com.telamin.fluxtion.runtime.audit.EventLogManager;
import com.telamin.fluxtion.runtime.audit.EventLogNode;
import com.telamin.fluxtion.runtime.time.Clock;
import com.telamin.fluxtion.runtime.time.ClockStrategy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The command-line reader, end to end against a real file.
 *
 * <p>Filters are asserted to be <b>selective</b>, not merely to run: a filter that quietly matches
 * everything looks identical to one that works until someone relies on it.
 */
public class AuditLogToolTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path log;

    static class Book extends EventLogNode {
        void publish(double price, int qty) {
            auditLog.info("price", price).info("qty", qty);
        }
    }

    /** Three events at t=100, 200, 300 from two nodes, so time and name filters have something to cut. */
    @Before
    public void writeLog() throws IOException {
        log = folder.newFile("audit.flxa").toPath();
        long[] now = {100L};
        Clock clock = new Clock();
        clock.init();
        clock.setClockStrategy(new ClockStrategy.ClockStrategyEvent(() -> now[0]));

        EventLogManager manager = new EventLogManager();
        manager.clock = clock;
        try (OutputStream os = java.nio.file.Files.newOutputStream(log);
             BinaryLogWriter writer = new BinaryLogWriter(os)) {
            manager.setLogSink(writer);
            manager.init();
            manager.calculationLogConfig(new EventLogControlEvent(new BinaryLogRecord(clock, 4096)));
            Book equity = new Book();
            Book bond = new Book();
            manager.nodeRegistered(equity, "equityBook");
            manager.nodeRegistered(bond, "bondBook");
            for (int i = 0; i < 3; i++) {
                now[0] = 100L * (i + 1);
                Object e = new Object();
                clock.eventReceived(e);
                manager.eventReceived(e);
                equity.publish(1.0 + i, 10 + i);
                bond.publish(2.0 + i, 20 + i);
                manager.processingComplete();
            }
        }
    }

    private static class Run {
        String out;
        String err;
        int code;
    }

    private Run run(String... args) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        ByteArrayOutputStream e = new ByteArrayOutputStream();
        String[] full = new String[args.length + 1];
        System.arraycopy(args, 0, full, 0, args.length);
        full[args.length] = log.toString();
        Run r = new Run();
        r.code = AuditLogTool.run(full, new PrintStream(o, true), new PrintStream(e, true));
        r.out = o.toString();
        r.err = e.toString();
        return r;
    }

    private static int count(String haystack, String needle) {
        int n = 0, i = 0;
        while ((i = haystack.indexOf(needle, i)) >= 0) { n++; i += needle.length(); }
        return n;
    }

    @Test
    public void withNoFiltersEverythingIsPrinted() throws IOException {
        Run r = run();
        assertEquals(0, r.code);
        assertEquals("3 records", 3, count(r.out, "eventLogRecord:"));
        assertEquals("12 entries", 12, count(r.out, "        - "));
    }

    @Test
    public void aTimeRangeSelectsRecordsAndExcludesTheRest() throws IOException {
        Run r = run("--from", "200", "--to", "200");
        assertEquals("only the middle event", 1, count(r.out, "eventLogRecord:"));
        assertTrue(r.out.contains("logTime: 200"));
        assertFalse("must exclude t=100", r.out.contains("logTime: 100"));
        assertFalse("must exclude t=300", r.out.contains("logTime: 300"));
    }

    @Test
    public void aNodeGlobSelectsOnlyThatNodesEntries() throws IOException {
        Run r = run("--node", "equity*");
        assertEquals("all three records still match", 3, count(r.out, "eventLogRecord:"));
        assertEquals("but only that node's entries", 6, count(r.out, "equityBook"));
        assertFalse("the other node must be excluded", r.out.contains("bondBook"));
    }

    @Test
    public void aKeyGlobSelectsOnlyThatKey() throws IOException {
        Run r = run("--key", "price");
        assertEquals("one per node per event", 6, count(r.out, "price:"));
        assertFalse(r.out.contains("qty:"));
    }

    @Test
    public void filtersCombine() throws IOException {
        Run r = run("--node", "bond*", "--key", "qty", "--from", "300");
        assertEquals(1, count(r.out, "eventLogRecord:"));
        assertEquals(1, count(r.out, "        - "));
        assertTrue(r.out.contains("bondBook"));
        assertTrue(r.out.contains("qty: 22"));
    }

    @Test
    public void limitStopsAfterNRecords() throws IOException {
        Run r = run("--limit", "2");
        assertEquals(2, count(r.out, "eventLogRecord:"));
    }

    /**
     * A pattern nothing can match must say so, rather than presenting an empty log as a result — and
     * must state the SCOPE of that claim.
     *
     * <p>It used to say "no name in this log matches", which is false whenever the name exists under
     * another {@code --event} or outside the time range: entries are only observed for records those
     * filters admitted, so the diagnostic can only speak for the current selection.
     */
    @Test
    public void anUnmatchablePatternIsReportedWithItsScope() throws IOException {
        Run r = run("--node", "noSuchBook*");
        assertEquals("nothing matched", 0, count(r.out, "eventLogRecord:"));
        assertTrue(r.err, r.err.contains("noSuchBook*"));
        assertTrue("the diagnostic must scope its claim to the selection:\n" + r.err,
                r.err.contains("records this query selected"));
        assertFalse("and must not claim whole-file absence:\n" + r.err,
                r.err.contains("no name in this log matches"));
    }

    @Test
    public void statsReportWhatWasReadAndWhatCouldNotBe() throws IOException {
        Run r = run("--stats", "--node", "equity*");
        assertTrue(r.err, r.err.contains("records read      : 3"));
        assertTrue(r.err, r.err.contains("entries matched   : 6"));
        assertTrue(r.err, r.err.contains("unresolved ids    : 0"));
        assertTrue(r.err, r.err.contains("unreadable bytes  : 0"));
    }

    @Test
    public void theNullSinkReadsWithoutPrinting() throws IOException {
        Run r = run("--sink", "null", "--stats");
        assertEquals("", r.out);
        assertTrue(r.err.contains("records matched   : 3"));
    }

    @Test
    public void aMissingFileIsReportedNotThrown() throws IOException {
        Run r = new Run();
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        ByteArrayOutputStream e = new ByteArrayOutputStream();
        r.code = AuditLogTool.run(new String[]{"/no/such/file.flxa"},
                new PrintStream(o, true), new PrintStream(e, true));
        assertEquals(2, r.code);
        assertTrue(e.toString(), e.toString().contains("cannot read"));
    }

    @Test
    public void anUnknownOptionIsRejectedWithUsage() throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        ByteArrayOutputStream e = new ByteArrayOutputStream();
        int code = AuditLogTool.run(new String[]{"--wat", "x", log.toString()},
                new PrintStream(o, true), new PrintStream(e, true));
        assertEquals(2, code);
        assertTrue(e.toString(), e.toString().contains("unknown option"));
    }

    @Test
    public void globMatchingHandlesStarAndQuestionMark() {
        assertTrue(AuditLogFilter.matches("equity*", "equityBook"));
        assertTrue(AuditLogFilter.matches("*Book", "equityBook"));
        assertTrue(AuditLogFilter.matches("*", "anything"));
        assertTrue(AuditLogFilter.matches("equityBoo?", "equityBook"));
        assertTrue(AuditLogFilter.matches("e*i*Book", "equityBook"));
        assertFalse(AuditLogFilter.matches("equity", "equityBook"));
        assertFalse(AuditLogFilter.matches("bond*", "equityBook"));
        assertFalse(AuditLogFilter.matches("equityBoo?", "equityBooks"));
    }

    // ---- round 5: the bounds are milliseconds, the file's unit is whatever its header says ----

    /** A copy of the fixture with its header unit set to {@code code}; nothing past the header changes. */
    private Path withUnit(int code) throws IOException {
        byte[] bytes = java.nio.file.Files.readAllBytes(log);
        bytes[6] = (byte) (code >>> 8);
        bytes[7] = (byte) code;
        Path copy = folder.newFile("unit-" + code + ".flxa").toPath();
        java.nio.file.Files.write(copy, bytes);
        return copy;
    }

    private Run runOn(Path file, String... args) throws IOException {
        Path saved = log;
        log = file;
        try {
            return run(args);
        } finally {
            log = saved;
        }
    }

    /**
     * REVIEWER PROBE (round 5). A millisecond range holding 2026 over a file DECLARING nanoseconds
     * matched nothing, exit 0, no warning: the bounds were compared to the readings as they stood.
     * The fixture's logTimes are 100, 200, 300 in the file's unit; under a nanosecond header those are
     * fractions of a millisecond, so a bound of one millisecond admits all three and a bound starting
     * at one millisecond admits none. The same file under a millisecond header keeps its old answer.
     */
    @Test
    public void millisecondBoundsAreScaledToTheFilesDeclaredUnit() throws IOException {
        Path nanos = withUnit(com.telamin.fluxtion.runtime.audit.BinaryLogFile.TIME_UNIT_EPOCH_NANOS);
        Run all = runOn(nanos, "--from", "0", "--to", "1");
        assertEquals(0, all.code);
        assertEquals("100, 200 and 300 ns all lie inside [0 ms, 1 ms]", 3, count(all.out, "eventLogRecord:"));
        Run none = runOn(nanos, "--from", "1", "--to", "2");
        assertEquals(0, none.code);
        assertEquals("nothing lies inside [1 ms, 2 ms]", 0, count(none.out, "eventLogRecord:"));
        Run middle = runOn(withUnit(com.telamin.fluxtion.runtime.audit.BinaryLogFile.TIME_UNIT_EPOCH_MILLIS),
                "--from", "200", "--to", "200");
        assertEquals("a millisecond file is unchanged", 1, count(middle.out, "eventLogRecord:"));

        // And the reviewer's own shape: a real 2026 nanosecond reading, a real 2026 millisecond range.
        Path real = folder.newFile("real-nanos.flxa").toPath();
        long reading = 1_789_151_371_851_762_250L;
        Clock clock = new Clock();
        clock.init();
        clock.setClockStrategy(new ClockStrategy.ClockStrategyEvent(() -> reading));
        try (OutputStream os = java.nio.file.Files.newOutputStream(real);
             BinaryLogWriter writer = new BinaryLogWriter(os,
                     com.telamin.fluxtion.runtime.audit.BinaryLogFile.TIME_UNIT_EPOCH_NANOS)) {
            BinaryLogRecord r = new BinaryLogRecord(clock, 4096);
            r.updateLogLevel(EventLogControlEvent.LogLevel.INFO);
            clock.eventReceived(new Object());
            r.triggerObject(new Object());
            r.addRecord("n", "k", 1);
            r.terminateRecord();
            writer.processLogRecord(r);
        }
        Run hit = runOn(real, "--from", "1000000000000", "--to", "2000000000000", "--stats");
        assertEquals(0, hit.code);
        assertTrue(hit.err, hit.err.contains("records matched   : 1"));
        assertTrue(hit.err, hit.err.contains("time unit         : epoch nanoseconds"));
        Run miss = runOn(real, "--from", "2000000000001", "--to", "3000000000000", "--stats");
        assertTrue(miss.err, miss.err.contains("records matched   : 0"));
    }

    /**
     * A file whose header states no unit cannot honour a millisecond query, so the query is refused
     * with the exit code a script can see - not answered with zero matches. Inspection without bounds
     * still works, and the stats line says what to do.
     */
    @Test
    public void aTimeQueryOnAFileWithNoDeclaredUnitIsRefused() throws IOException {
        Path undeclared = withUnit(com.telamin.fluxtion.runtime.audit.BinaryLogFile.TIME_UNIT_UNSPECIFIED);
        Run refused = runOn(undeclared, "--from", "200", "--to", "200");
        assertEquals(2, refused.code);
        assertEquals("nothing printed for a refused query", "", refused.out);
        assertTrue(refused.err, refused.err.contains("--declare-unit"));

        Run raw = runOn(undeclared, "--stats");
        assertEquals("no bounds, no unit needed", 0, raw.code);
        assertEquals(3, count(raw.out, "eventLogRecord:"));
        assertTrue(raw.err, raw.err.contains("unspecified"));
        assertTrue(raw.err, raw.err.contains("--declare-unit"));

        Path undefined = withUnit(9);
        Run undefinedQuery = runOn(undefined, "--from", "200", "--to", "200");
        assertEquals(2, undefinedQuery.code);
        assertTrue(undefinedQuery.err, undefinedQuery.err.contains("code 9"));
    }

    /**
     * The unit is established by the USER, into the evidence: a copy whose header states it. Only a
     * header that states none is filled in; a stated unit is the producer's claim and is not rewritten.
     */
    @Test
    public void declareUnitWritesACopyThatEveryReaderThenTrusts() throws IOException {
        Path undeclared = withUnit(com.telamin.fluxtion.runtime.audit.BinaryLogFile.TIME_UNIT_UNSPECIFIED);
        Path copy = folder.getRoot().toPath().resolve("declared.flxa");
        Run declared = runOn(undeclared, "--declare-unit", "nanos", "--out", copy.toString());
        assertEquals(declared.err, 0, declared.code);
        assertTrue(declared.out, declared.out.contains("epoch nanoseconds"));

        byte[] original = java.nio.file.Files.readAllBytes(undeclared);
        byte[] written = java.nio.file.Files.readAllBytes(copy);
        assertEquals("the original is untouched", 0, original[7]);
        assertEquals(com.telamin.fluxtion.runtime.audit.BinaryLogFile.TIME_UNIT_EPOCH_NANOS, written[7]);
        assertEquals(original.length, written.length);
        for (int i = 8; i < original.length; i++) {
            assertEquals("byte " + i + " past the header is unchanged", original[i], written[i]);
        }
        Run stats = runOn(copy, "--stats", "--from", "0", "--to", "1");
        assertEquals(0, stats.code);
        assertTrue(stats.err, stats.err.contains("time unit         : epoch nanoseconds"));
        assertTrue(stats.err, stats.err.contains("records matched   : 3"));

        Run again = runOn(copy, "--declare-unit", "millis", "--out", copy.toString() + ".2");
        assertEquals("a stated unit is not rewritten", 2, again.code);
        assertTrue(again.err, again.err.contains("already states"));
        Run clobber = runOn(undeclared, "--declare-unit", "millis", "--out", copy.toString());
        assertEquals("an existing file is not overwritten", 2, clobber.code);
        Run badUnit = runOn(undeclared, "--declare-unit", "seconds", "--out", copy.toString() + ".3");
        assertEquals(2, badUnit.code);
        Run noOut = runOn(undeclared, "--declare-unit", "millis");
        assertEquals(2, noOut.code);
    }

    /** Writes one nanosecond-declared record whose logTime is {@code reading}. */
    private Path nanosFileAt(long reading, String name) throws IOException {
        Path file = folder.newFile(name).toPath();
        Clock clock = new Clock();
        clock.init();
        clock.setClockStrategy(new ClockStrategy.ClockStrategyEvent(() -> reading));
        try (OutputStream os = java.nio.file.Files.newOutputStream(file);
             BinaryLogWriter writer = new BinaryLogWriter(os,
                     com.telamin.fluxtion.runtime.audit.BinaryLogFile.TIME_UNIT_EPOCH_NANOS)) {
            BinaryLogRecord r = new BinaryLogRecord(clock, 4096);
            r.updateLogLevel(EventLogControlEvent.LogLevel.INFO);
            clock.eventReceived(new Object());
            r.triggerObject(new Object());
            r.addRecord("n", "k", 1);
            r.terminateRecord();
            writer.processLogRecord(r);
        }
        return file;
    }

    /**
     * REVIEWER PROBE (round 6). Clamping an out-of-domain bound to Long.MAX_VALUE turned "after every
     * instant" into "at the last instant", and a record stamped exactly there matched. The inequality
     * is kept: a lower bound above every representable nanosecond admits nothing, an upper bound
     * below every one admits nothing, and the neighbouring in-domain bounds still admit the record.
     */
    @Test
    public void anOutOfDomainBoundKeepsItsInequality_neverInventingAMatch() throws IOException {
        Path atMax = nanosFileAt(Long.MAX_VALUE, "max-nanos.flxa");
        assertTrue(runOn(atMax, "--from", "9223372036855", "--stats").err.contains("records matched   : 0"));
        assertTrue(runOn(atMax, "--from", "9223372036854", "--stats").err.contains("records matched   : 1"));
        assertTrue(runOn(atMax, "--to", "9223372036855", "--stats").err.contains("records matched   : 1"));
        Path atMin = nanosFileAt(Long.MIN_VALUE, "min-nanos.flxa");
        assertTrue(runOn(atMin, "--to", "-9223372036855", "--stats").err.contains("records matched   : 0"));
        assertTrue(runOn(atMin, "--to", "-9223372036854", "--stats").err.contains("records matched   : 1"));
        assertTrue(runOn(atMin, "--from", "-9223372036855", "--stats").err.contains("records matched   : 1"));
    }

    /** An explicitly supplied extreme is a bound, not the "no bound" sentinel it used to be. */
    @Test
    public void anExplicitExtremeBoundIsABound() throws IOException {
        Path undeclared = withUnit(com.telamin.fluxtion.runtime.audit.BinaryLogFile.TIME_UNIT_UNSPECIFIED);
        Run r = runOn(undeclared, "--from", String.valueOf(Long.MIN_VALUE));
        assertEquals("a bound was given, so a file with no unit refuses the query", 2, r.code);
        Run none = runOn(undeclared);
        assertEquals("no bound given: raw inspection still works", 0, none.code);
    }
}
