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
     * Selecting BINARY with no sink installed used to build, start, and then throw from inside the
     * first event cycle. It refuses at init now, naming the fix.
     */
    @Test
    public void binaryWithTheDefaultSinkRefusesAtInitNamingTheFix() {
        EventLogManager manager = new EventLogManager().tracingOff().binaryRecord(true);
        try {
            manager.init();
            fail("binary records with the implicit println sink must be refused at init");
        } catch (IllegalStateException refused) {
            String m = refused.getMessage();
            assertTrue("must name the sink type to install, got: " + m, m.contains("BinaryLogWriter"));
            assertTrue("must offer the text alternative, got: " + m, m.contains("TEXT"));
        }
    }
}
