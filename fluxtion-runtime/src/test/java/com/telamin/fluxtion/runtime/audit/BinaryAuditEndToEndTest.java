package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.audit.tools.AuditLogFilter;
import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;
import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * The binary audit path driven END TO END, through the surfaces a user actually touches.
 *
 * <p><b>Why these are new.</b> A review found five defects on this path while every existing test was
 * green, because each of those tests stopped at a component boundary: one asserted the manager built a
 * binary record and stopped before publishing, another proved a String reached storage using its own
 * dictionary renderer rather than the production one. Each defect lived in the gap between two tested
 * pieces. These drive logger → record → writer → reader → CLI filter instead.
 */
public class BinaryAuditEndToEndTest {

    private EventLogManager binaryManager(LogRecordListener sink) {
        EventLogManager manager = new EventLogManager(sink).tracingOff().binaryRecord(true);
        manager.init();
        return manager;
    }

    /** The record a node logs into, wired the way the manager wires one. */
    private BinaryLogRecord record() {
        Clock clock = new Clock();
        clock.init();
        BinaryLogRecord r = new BinaryLogRecord(clock, 4096);
        r.updateLogLevel(LogLevel.INFO);
        r.triggerObject(new Object());
        return r;
    }

    /** A BinaryEventLogger with its own level set - a logger with no level records nothing. */
    private EventLogger loggerOn(BinaryLogRecord r) {
        EventLogger logger = new BinaryEventLogger(r, "node");
        logger.setLevel(LogLevel.INFO);
        return logger;
    }

    /**
     * A char is the one primitive that had no id/slot path, so it went to a byte buffer the binary
     * record abandons: the record called itself publishable and the file declared zero entries.
     */
    @Test
    public void aCharSurvivesToTheFile() throws Exception {
        Path file = Files.createTempFile("audit-char", ".flxa");
        BinaryLogRecord r = record();
        EventLogger logger = loggerOn(r);
        logger.info("c", 'x');
        r.terminateRecord();

        try (BinaryLogWriter w = new BinaryLogWriter(Files.newOutputStream(file))) {
            w.processLogRecord(r);
        }
        List<String> values = new ArrayList<>();
        BinaryLogReader.Result result = BinaryLogReader.read(file, new BinaryLogReader.Visitor() {
            public boolean onRecord(int a, String b, long c, long d, long e, int f) { return true; }
            public void onEntry(int nodeId, String node, int keyId, String key, int tag, long bits) {
                values.add(BinaryRecordDecoder.renderValue(tag, bits));
            }
        });
        assertEquals("the char entry must reach the file", 1, result.entries);
        assertEquals("and decode as its character", "x", values.get(0));
    }

    /**
     * String values are stored as dictionary ids. The shipped CLI renderer had no case for that tag and
     * printed the raw pair, so a real String showed up as {@code #tag5:4} rather than its text.
     */
    @Test
    public void aStringRendersAsItsValueThroughTheProductionFilter() throws Exception {
        Path file = Files.createTempFile("audit-string", ".flxa");
        BinaryLogRecord r = record();
        EventLogger logger = loggerOn(r);
        logger.info("venue", "DEMO");
        r.terminateRecord();
        try (BinaryLogWriter w = new BinaryLogWriter(Files.newOutputStream(file))) {
            w.processLogRecord(r);
        }

        List<String> rendered = new ArrayList<>();
        AuditLogFilter filter = new AuditLogFilter(null, null, null, Long.MIN_VALUE, Long.MAX_VALUE,
                Long.MAX_VALUE, new AuditLogFilter.Sink() {
            public void record(String eventType, long eventTime, long logTime, long endTime) { }
            public void entry(String node, String key, String value) { rendered.add(value); }
        });
        BinaryLogReader.read(file, filter);

        assertEquals("the production CLI path must render the string, not its tag/id pair",
                "DEMO", rendered.get(0));
    }

    /** A truncated record must not be written as a complete one. */
    @Test
    public void anOverflowedRecordIsRefusedRatherThanWrittenShort() {
        BinaryLogRecord r = record();
        EventLogger logger = loggerOn(r);
        // LITERAL keys. Names are interned by identity - a computed key is a new instance every call
        // and would exhaust the dictionary instead of overflowing the record, which is a different
        // defect with its own refusal.
        for (int i = 0; i < 100_000; i++) {
            logger.info("k", i);
        }
        assumeOverflowed(r);
        r.terminateRecord();
        try (BinaryLogWriter w = new BinaryLogWriter(new ByteArrayOutputStream())) {
            w.processLogRecord(r);
            fail("a record that dropped entries must not be written as a complete one");
        } catch (IllegalStateException refused) {
            assertTrue("the refusal must say entries were dropped, got: " + refused.getMessage(),
                    refused.getMessage().contains("overflowed"));
        } catch (Exception e) {
            fail("expected refusal, got " + e);
        }
    }

    private void assumeOverflowed(BinaryLogRecord r) {
        org.junit.Assume.assumeTrue("this fixture must overflow to be meaningful", r.overflowed());
    }

    /** A dictionary name longer than the u16 length field corrupted the file rather than being refused. */
    @Test
    public void anOversizeDictionaryNameIsRefusedRatherThanCorruptingTheFile() {
        BinaryLogRecord r = record();
        EventLogger logger = loggerOn(r);
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 70_000; i++) {
            huge.append('a');
        }
        logger.info("big", huge.toString());
        r.terminateRecord();
        try (BinaryLogWriter w = new BinaryLogWriter(new ByteArrayOutputStream())) {
            w.processLogRecord(r);
            fail("a name too long for the u16 length field must be refused, not silently truncated");
        } catch (IllegalStateException refused) {
            assertTrue("the refusal must name the limit, got: " + refused.getMessage(),
                    refused.getMessage().contains("65535"));
        } catch (Exception e) {
            fail("expected refusal, got " + e);
        }
    }

    /**
     * No sink installed: the refusal comes from the DEFAULT SINK at publish, naming the fix.
     *
     * <p>It was briefly refused at {@code init()} instead, which broke every generated processor:
     * generated code calls {@code EventLogManager.init()} from the PROCESSOR'S CONSTRUCTOR, so the
     * guard fired before any caller could retrieve the auditor and install a sink — 22 errors in the
     * compiler suite. The documented sequence is construct, retrieve the auditor, install the writer,
     * then {@code processor.init()}. Refusing at publish keeps that window open.
     */
    @Test
    public void binaryWithTheDefaultSinkRefusesAtPublishNamingTheFix() {
        EventLogManager manager = new EventLogManager().tracingOff().binaryRecord(true);
        manager.clock = new Clock();
        manager.clock.init();
        manager.init();     // MUST NOT throw - this is the generated processor's constructor
        manager.nodeRegistered(new Object(), "node");
        manager.eventReceived(new Object());
        try {
            manager.publishLastRecord();
            fail("a binary record reaching the default text sink must be refused");
        } catch (IllegalStateException refused) {
            String m = refused.getMessage();
            assertTrue("must name the sink type to install, got: " + m, m.contains("BinaryLogWriter"));
            assertTrue("must offer the text alternative, got: " + m, m.contains("TEXT"));
        }
    }

    /**
     * The STRING-KEY primitive overloads must reach the file.
     *
     * <p>They wrote to the byte buffer this record does not publish, so every one of them produced a
     * record that called itself publishable and a file declaring zero entries. char was fixed on the
     * INDEXED path and this family was left behind.
     */
    @Test
    public void stringKeyPrimitivesReachTheFile() throws Exception {
        Path file = Files.createTempFile("audit-stringkey", ".flxa");
        BinaryLogRecord r = record();
        r.addRecord("node", "d", 1.25d);
        r.addRecord("node", "l", 7L);
        r.addRecord("node", "i", 3);
        r.addRecord("node", "c", 'z');
        r.addRecord("node", "b", true);
        assertTrue("the record must report itself publishable", r.terminateRecord());
        try (BinaryLogWriter w = new BinaryLogWriter(Files.newOutputStream(file))) {
            w.processLogRecord(r);
        }
        List<String> values = new ArrayList<>();
        BinaryLogReader.Result result = BinaryLogReader.read(file, new BinaryLogReader.Visitor() {
            public boolean onRecord(int a, String b, long c, long d, long e, int f) { return true; }
            public void onEntry(int nodeId, String node, int keyId, String key, int tag, long bits) {
                values.add(BinaryRecordDecoder.renderValue(tag, bits));
            }
        });
        assertEquals("all five string-key primitives must reach the file", 5, result.entries);
        assertEquals("[1.25, 7, 3, z, true]", values.toString());
    }

    /**
     * A record REPLACED at runtime keeps writing a correct file.
     *
     * <p>{@code EventLogControlEvent} can swap the {@code LogRecord} without swapping the sink, and
     * the runtime documents that as supported. The new record re-interns names, so the same graph can
     * allocate the same ids to different names — which silently relabelled every later entry, then
     * (briefly) was refused outright. The writer translates instead: names are the identity.
     */
    @Test
    public void aReplacementRecordStillWritesCorrectNames() throws Exception {
        Path file = Files.createTempFile("audit-swap", ".flxa");
        Clock clock = new Clock();
        clock.init();
        try (BinaryLogWriter w = new BinaryLogWriter(Files.newOutputStream(file))) {
            BinaryLogRecord first = new BinaryLogRecord(clock, 4096);
            first.updateLogLevel(LogLevel.INFO);
            first.triggerObject(new Object());
            first.addRecord("z", "k", 1);
            first.terminateRecord();
            w.processLogRecord(first);

            // A DIFFERENT record instance, interning a different name into the same id.
            BinaryLogRecord second = new BinaryLogRecord(clock, 4096);
            second.updateLogLevel(LogLevel.INFO);
            second.triggerObject(new Object());
            second.addRecord("a", "k", 2);
            second.terminateRecord();
            w.processLogRecord(second);
        }
        List<String> nodes = new ArrayList<>();
        BinaryLogReader.read(file, new BinaryLogReader.Visitor() {
            public boolean onRecord(int a, String b, long c, long d, long e, int f) { return true; }
            public void onEntry(int nodeId, String node, int keyId, String key, int tag, long bits) {
                nodes.add(node);
            }
        });
        assertEquals("each entry must carry the name its own record used", "[z, a]", nodes.toString());
    }

    /** The dictionary-exhaustion refusal, pinned. */
    @Test
    public void anExhaustedDictionaryIsRefusedNamingTheIdentityTrap() {
        BinaryLogRecord r = record();
        try {
            for (int i = 0; i < Short.MAX_VALUE + 10; i++) {
                r.addRecord("node", "k", new String("v" + i));   // a fresh instance every call
            }
            fail("exhausting the dictionary must be refused, not wrapped to a negative id");
        } catch (IllegalStateException refused) {
            assertTrue("must explain identity interning, got: " + refused.getMessage(),
                    refused.getMessage().contains("IDENTITY"));
        }
    }
}
