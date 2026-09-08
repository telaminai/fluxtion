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

    /** A pattern nothing can match must say so, rather than presenting an empty log as a result. */
    @Test
    public void anUnmatchablePatternIsReportedRatherThanReturningSilence() throws IOException {
        Run r = run("--node", "noSuchBook*");
        assertEquals("nothing matched", 0, count(r.out, "eventLogRecord:"));
        assertTrue(r.err, r.err.contains("no name in this log matches"));
        assertTrue(r.err, r.err.contains("noSuchBook*"));
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
}
