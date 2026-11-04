/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.builder.replay;

import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;
import com.telamin.fluxtion.runtime.audit.EventLogger;
import com.telamin.fluxtion.runtime.audit.LogRecord;
import com.telamin.fluxtion.runtime.audit.StructuredLogRecord;
import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A string-valued audit entry must read back as exactly the string that was logged.
 *
 * <p>Text audit values sit in a YAML flow map, and were written bare: the String
 * {@code membership not active, balance due 30.0} was cut at the comma by the YAML reader and came
 * back as {@code membership not active}; {@code 123} came back as an Integer. This writes values that
 * contain every YAML-significant character through the real {@link LogRecord} path, then reads them
 * back with SnakeYAML and with {@link YamlLogRecordListener}.
 */
@SuppressWarnings("unchecked")
public class AuditValueYamlRoundTripTest {

    static final List<String> HOSTILE = Arrays.asList(
            "membership not active, balance due 30.0",
            "key: value",
            "a:b",
            "{braced}",
            "x}y{z",
            "[listy]",
            "a]b[c",
            "# not a comment",
            "trailing #hash",
            "say \"hi\"",
            "\"",
            "it's",
            "'",
            "back\\slash",
            "C:\\temp\\",
            "line1\nline2",
            "cr\rtab\t",
            " leading",
            "trailing ",
            " ",
            "-", "- item", "? q", "&anchor", "*alias", "!tag", "|", ">", "%", "@at", "`tick`", "=", "<<",
            "ctrl\u0001char", "del\u007f", "nel\u0085", "unicode \u00e9 \u2713 \u65e5\u672c",
            "MutableOrder(id=1, venue=null)",
            "ok?", "is it ok?", "url?x=1", "?",
            "open(", "close)", "a)b(c", "f(g(x)", "(", ")",
            "a \"b, c", "quote\"inside", "smile \uD83D\uDE00, ok", "\uD83D\uDE00",
            "sep\u2028x", "bom\uFEFF", "x\n    textEncoding: QUOTED_SCALARS"
    );

    /**
     * Strings that merely LOOK typed are written bare, exactly as runtime 1.1.3 wrote them, and read
     * back with YAML typing as they always did: "5000" is the number 5000, which the Analyser graphs.
     */
    static final List<String> TYPED = Arrays.asList(
            "5000", "true", "false", "True", "yes", "No", "on", "OFF", "null", "Null", "~", "",
            "123", "-5", "+7", "1.5", "1e3", "1e10", ".5", "0x1F", "1_000", "2024-01-01",
            ".inf", "-.inf", ".nan", "NaN", "Infinity", "\u0661\u0662\u0663",
            // a ':' not before a space or at the end is plain in a flow scalar: times, timestamps, URLs
            "12:30:00", "10:00", "2026-11-04T10:00:00Z", "2026-11-04T10:00:00.123456789Z",
            "2026-11-04T10:00:00+01:00", "2026-11-04T10:00", "http://x:8/a", "C:\\temp", "fe80::1"
    );

    /** Simple values must stay plain so existing logs, and diffs of them, do not move. */
    static final List<String> PLAIN = Arrays.asList(
            "simple", "ok", "membership active", "Tick->getPrice", "30 days", "abc123",
            "MutableOrder(id=1)", "a-b_c.d", "path/to/x", "100%ok", "unicode \u00e9", "back\\slash",
            "f(g(x))", "smile \uD83D\uDE00"
    );

    @Test
    public void hostileStringValuesRoundTripThroughSnakeYaml() {
        String yaml = writeRecord(HOSTILE);
        Map<String, Object> props = nodeProps(new Yaml().load(yaml), "gate");
        for (int i = 0; i < HOSTILE.size(); i++) {
            assertEquals("v" + i + " in:\n" + yaml, HOSTILE.get(i), props.get("v" + i));
        }
        assertEquals(yaml, HOSTILE.size(), props.size());
    }

    @Test
    public void hostileStringValuesRoundTripThroughYamlLogRecordListener() {
        LogRecord record = newRecord();
        EventLogger logger = logger(record, "gate");
        record.triggerObject(new Object());
        for (int i = 0; i < HOSTILE.size(); i++) {
            logger.info("v" + i, HOSTILE.get(i));
        }
        logger(record, "other").info("count", 3);
        record.terminateRecord();

        YamlLogRecordListener listener = new YamlLogRecordListener();
        listener.processLogRecord(record);
        // and the file path a stored log is read through
        YamlLogRecordListener fromFile = new YamlLogRecordListener();
        fromFile.loadFromFile(new StringReader(record.asCharSequence().toString() + "\n---\n"));

        for (YamlLogRecordListener l : Arrays.asList(listener, fromFile)) {
            assertEquals(1, l.getEventList().size());
            StructuredLogRecord parsed = l.getEventList().get(0);
            List<StructuredLogRecord.AuditRecord> audits = parsed.getAuditLogs();
            assertEquals(record.toString(), 2, audits.size());
            assertEquals("gate", audits.get(0).getNodeId());
            Map<?, ?> props = audits.get(0).getPropertyMap();
            for (int i = 0; i < HOSTILE.size(); i++) {
                assertEquals("v" + i + " in:\n" + record, HOSTILE.get(i), props.get("v" + i));
            }
            assertEquals(3, audits.get(1).getPropertyMap().get("count"));
        }
    }

    @Test
    public void typedLookingStringsAreWrittenBareAsIn113() {
        for (String value : TYPED) {
            String yaml = writeRecord(Arrays.asList(value));
            assertTrue("expected bare output: " + yaml, yaml.contains("\n        - gate: { v0: " + value + "}\n"));
            assertFalse(yaml, yaml.contains("textEncoding"));
            Object asIn113 = ((Map<?, ?>) new Yaml().load("{ v0: " + value + "}")).get("v0");
            assertEquals(yaml, asIn113, nodeProps(new Yaml().load(yaml), "gate").get("v0"));
        }
        // and with infoText the time-shaped ones are quoted and read back as Strings
        LogRecord record = newRecord();
        record.triggerObject(new Object());
        EventLogger text = logger(record, "gate");
        List<String> times = Arrays.asList("12:30:00", "2026-11-04T10:00:00Z", "2026-11-04 10:00:00");
        for (int i = 0; i < times.size(); i++) {
            text.infoText("v" + i, times.get(i));
        }
        record.terminateRecord();
        String marked = record.toString();
        assertTrue(marked, marked.startsWith("eventLogRecord: \n    textEncoding: QUOTED_SCALARS\n"));
        Map<String, Object> props = nodeProps(new Yaml().load(marked), "gate");
        for (int i = 0; i < times.size(); i++) {
            assertEquals(marked, times.get(i), props.get("v" + i));
        }
        // the regression that prompted this: a String "5000" reads as the number, as it always did
        assertEquals(5000, nodeProps(new Yaml().load(writeRecord(Arrays.asList("5000"))), "gate").get("v0"));
    }

    @Test
    public void simpleStringValuesStayPlain() {
        for (String value : PLAIN) {
            String yaml = writeRecord(Arrays.asList(value));
            assertTrue("expected plain output: " + yaml, yaml.contains("{ v0: " + value + "}"));
            assertEquals(value, nodeProps(new Yaml().load(yaml), "gate").get("v0"));
        }
    }

    @Test
    public void typedValuesStayTypedAndUnquoted() {
        LogRecord record = newRecord();
        EventLogger logger = logger(record, "gate");
        record.triggerObject(new Object());
        logger.info("i", 42);
        logger.info("l", -7L);
        logger.info("d", 30.5);
        logger.info("b", true);
        logger.info("boxed", (Object) Integer.valueOf(9));
        logger.info("boxedBool", (Object) Boolean.FALSE);
        logger.info("nullObj", (Object) null);
        logger.info("nullStr", (CharSequence) null);
        logger.info("ch", 'a');
        logger.info("chComma", ',');
        logger.info("chDigit", '7');
        logger.info("obj", (Object) new Thing("x, y: z"));
        logger.info("sb", new StringBuilder("a, b"));
        record.terminateRecord();
        String yaml = record.toString();

        assertTrue(yaml, yaml.contains("{ i: 42, l: -7, d: 30.5, b: true, boxed: 9, boxedBool: false,"
                + " nullObj: null, nullStr: null, ch: a,"));

        Map<String, Object> props = nodeProps(new Yaml().load(yaml), "gate");
        assertEquals(42, props.get("i"));
        assertEquals(-7, props.get("l"));
        assertEquals(30.5, props.get("d"));
        assertEquals(true, props.get("b"));
        assertEquals(9, props.get("boxed"));
        assertEquals(false, props.get("boxedBool"));
        assertTrue(props.containsKey("nullObj"));
        assertNull(props.get("nullObj"));
        assertNull(props.get("nullStr"));
        assertEquals("a", props.get("ch"));
        assertEquals(",", props.get("chComma"));
        assertEquals("a digit char is bare, as in 1.1.3", 7, props.get("chDigit"));
        assertEquals("Thing[x, y: z]", props.get("obj"));
        assertEquals("a, b", props.get("sb"));
    }

    @Test
    public void markerPresentIffQuotedAndReadersIgnoreIt() {
        // three records in one stream: quoted, plain, quoted - state must reset per record
        LogRecord record = newRecord();
        EventLogger gate = logger(record, "gate");
        EventLogger other = logger(record, "other");
        StringBuilder stream = new StringBuilder();

        record.triggerObject(new Object());
        gate.info("a", "plain");
        other.info("reason", "not active, balance due 30.0");
        other.info("n", 1);
        record.terminateRecord();
        String first = record.toString();
        stream.append(first).append("\n---\n");
        record.clear();

        record.triggerObject(new Object());
        gate.info("a", "plain");
        other.info("n", 2);
        record.terminateRecord();
        String second = record.toString();
        stream.append(second).append("\n---\n");
        record.clear();

        record.triggerObject(new Object());
        gate.info("q", "say \"hi\", ok?");
        record.terminateRecord();
        String third = record.toString();
        stream.append(third).append("\n---\n");

        assertTrue(first, first.startsWith("eventLogRecord: \n    textEncoding: QUOTED_SCALARS\n"));
        assertFalse(second, second.contains("textEncoding"));
        assertTrue(third, third.startsWith("eventLogRecord: \n    textEncoding: QUOTED_SCALARS\n"));

        // SnakeYAML: the marker is a header key; nodeLogs values are exact and do not include it
        Map<String, Object> rec = (Map<String, Object>) ((Map<String, Object>) new Yaml().load(first)).get("eventLogRecord");
        assertEquals("QUOTED_SCALARS", rec.get("textEncoding"));
        assertEquals("not active, balance due 30.0", nodeProps(new Yaml().load(first), "other").get("reason"));
        assertFalse(nodeProps(new Yaml().load(first), "other").containsKey("textEncoding"));
        assertEquals("say \"hi\", ok?", nodeProps(new Yaml().load(third), "gate").get("q"));

        // YamlLogRecordListener, file path: three records, marker does not leak into nodeLogs
        YamlLogRecordListener listener = new YamlLogRecordListener();
        listener.loadFromFile(new StringReader(stream.toString()));
        List<StructuredLogRecord> events = listener.getEventList();
        assertEquals(3, events.size());
        for (StructuredLogRecord e : events) {
            for (StructuredLogRecord.AuditRecord audit : e.getAuditLogs()) {
                assertFalse(audit.getPropertyMap().containsKey("textEncoding"));
                assertTrue(audit.getNodeId(), audit.getNodeId().equals("gate") || audit.getNodeId().equals("other"));
            }
            assertEquals("Object", e.getEventType());
        }
        assertEquals(2, events.get(0).getAuditLogs().size());
        assertEquals("not active, balance due 30.0", events.get(0).getAuditLogs().get(1).getPropertyMap().get("reason"));
        assertEquals(2, events.get(1).getAuditLogs().get(1).getPropertyMap().get("n"));
        assertEquals("say \"hi\", ok?", events.get(2).getAuditLogs().get(0).getPropertyMap().get("q"));
        assertTrue(events.get(0).getLogTime() >= 0);

        // and the in-process path
        YamlLogRecordListener direct = new YamlLogRecordListener();
        direct.processLogRecord(record);
        assertEquals("say \"hi\", ok?", direct.getEventList().get(0).getAuditLogs().get(0).getPropertyMap().get("q"));
    }

    @Test
    public void quotedHeaderFieldsRoundTrip() {
        LogRecord record = newRecord();
        record.groupingId = "grp: 1, #2";
        record.printEventToString(true);
        record.setPrintThreadName(true);
        record.triggerObject(new Thing("x\n    textEncoding: forged, a: b"));
        logger(record, "gate").info("v", 1);
        record.terminateRecord();
        String yaml = record.toString();
        Map<String, Object> rec = (Map<String, Object>) ((Map<String, Object>) new Yaml().load(yaml)).get("eventLogRecord");
        assertEquals(yaml, "QUOTED_SCALARS", rec.get("textEncoding"));
        assertEquals("grp: 1, #2", rec.get("groupingId"));
        assertEquals("Thing[x\n    textEncoding: forged, a: b]", rec.get("eventToString"));
        assertEquals(Thread.currentThread().getName(), rec.get("thread"));
        YamlLogRecordListener listener = new YamlLogRecordListener();
        listener.processLogRecord(record);
        assertEquals("grp: 1, #2", listener.getEventList().get(0).getGroupingId());
    }

    @Test
    public void loneSurrogateBecomesReplacementCharacter() {
        String yaml = writeRecord(Arrays.asList("lone \uD800 x"));
        assertTrue(yaml, yaml.startsWith("eventLogRecord: \n    textEncoding: QUOTED_SCALARS\n"));
        assertEquals("lone \uFFFD x", nodeProps(new Yaml().load(yaml), "gate").get("v0"));
    }

    /**
     * Values logged with {@code infoText} read back as exactly the logged String in SnakeYAML and
     * YamlLogRecordListener, even when they look like numbers; the same values via {@code info} keep
     * YAML typing. The Analyser reads a marked record's quoted scalars as Strings the same way (Format 1
     * rev 1.2, section 3a), so 472.1 and 472.10 stay distinct ids there too.
     */
    @Test
    public void textApiValuesReadBackAsStrings() {
        List<String> values = Arrays.asList("472.1", "472.10", "true", "null", "", "5000", "0x1F", "~", "a, b");
        LogRecord record = newRecord();
        record.triggerObject(new Object());
        EventLogger text = logger(record, "ctl");
        EventLogger plain = logger(record, "plain");
        for (int i = 0; i < values.size(); i++) {
            text.infoText("v" + i, values.get(i));
        }
        for (int i = 0; i < values.size(); i++) {
            plain.info("v" + i, values.get(i));
        }
        record.terminateRecord();
        String yaml = record.toString();
        assertTrue(yaml, yaml.startsWith("eventLogRecord: \n    textEncoding: QUOTED_SCALARS\n"));

        Map<String, Object> textProps = nodeProps(new Yaml().load(yaml), "ctl");
        Map<String, Object> plainProps = nodeProps(new Yaml().load(yaml), "plain");
        for (int i = 0; i < values.size(); i++) {
            assertEquals(yaml, values.get(i), textProps.get("v" + i));
        }
        assertEquals(472.1, plainProps.get("v0"));
        assertEquals(472.1, plainProps.get("v1"));   // the collision infoText exists to avoid
        assertEquals(5000, plainProps.get("v5"));
        assertNull(plainProps.get("v3"));

        YamlLogRecordListener listener = new YamlLogRecordListener();
        listener.processLogRecord(record);
        Map<?, ?> parsed = listener.getEventList().get(0).getAuditLogs().get(0).getPropertyMap();
        for (int i = 0; i < values.size(); i++) {
            assertEquals(values.get(i), parsed.get("v" + i));
        }
    }

    /** Typed-looking header values are bare and unmarked, reading with YAML typing as in 1.1.3. */
    @Test
    public void typedLookingHeaderValuesAreBare() {
        for (String v : Arrays.asList("123", "12:30:00", "2001-12-14 21:59:43.10 -5", "true", "null", "-dash", "0x1F")) {
            LogRecord record = newRecord();
            record.groupingId = v;
            record.triggerObject(new Object());
            logger(record, "gate").info("n", 1);
            record.terminateRecord();
            String yaml = record.toString();
            assertTrue(yaml, yaml.contains("\n    groupingId: " + v + "\n"));
            assertFalse(yaml, yaml.contains("textEncoding"));
            Object asIn113 = ((Map<?, ?>) new Yaml().load("groupingId: " + v)).get("groupingId");
            Map<String, Object> rec = (Map<String, Object>) ((Map<String, Object>) new Yaml().load(yaml)).get("eventLogRecord");
            assertEquals(yaml, asIn113, rec.get("groupingId"));
        }
    }

    /** Header fields use the block rule: typical toStrings stay plain, and everything round-trips. */
    @Test
    public void headerValuesRoundTripUnderTheBlockRule() {
        List<String> plain = Arrays.asList("Trade[id=1, px=2.0]", "X(a=1, b=2)",
                "OrderEvent{id='o1', symbol='AAPL', qty=100}", "a:b", "x#y", "k=v, [list], {map}", "unbalanced(",
                "ok?", "30 days");
        List<String> quoted = Arrays.asList("a: b", "x #y", "ends:", " lead", "trail ", "\u3000wide", "nbsp\u00a0",
                "[leading", "- dash", "\"q", "two\nlines", "tab\tx", "smile \uD83D\uDE00, x: y",
                "x\n    textEncoding: forged");
        for (List<String> set : Arrays.asList(plain, quoted)) {
            for (String v : set) {
                LogRecord record = newRecord();
                record.groupingId = v;
                record.printEventToString(true);
                record.triggerObject(new Thing(v) {
                    @Override
                    public String toString() {
                        return v;
                    }
                });
                logger(record, "gate").info("n", 1);
                record.terminateRecord();
                String yaml = record.toString();
                boolean marked = yaml.startsWith("eventLogRecord: \n    textEncoding: QUOTED_SCALARS\n");
                assertEquals(yaml, set == quoted, marked);
                Map<String, Object> rec = (Map<String, Object>) ((Map<String, Object>) new Yaml().load(yaml)).get("eventLogRecord");
                assertEquals(yaml, v, rec.get("eventToString"));
                assertEquals(yaml, v, rec.get("groupingId"));
                assertEquals(yaml, marked ? "QUOTED_SCALARS" : null, rec.get("textEncoding"));
                if (set == plain) {
                    assertTrue(yaml, yaml.contains("\n    eventToString: " + v + "\n"));
                }
            }
        }
    }

    static class Thing {
        final String s;

        Thing(String s) {
            this.s = s;
        }

        @Override
        public String toString() {
            return "Thing[" + s + "]";
        }
    }

    private static String writeRecord(List<String> values) {
        LogRecord record = newRecord();
        EventLogger logger = logger(record, "gate");
        record.triggerObject(new Object());
        for (int i = 0; i < values.size(); i++) {
            logger.info("v" + i, values.get(i));
        }
        record.terminateRecord();
        return record.toString();
    }

    private static LogRecord newRecord() {
        Clock clock = new Clock();
        clock.init();
        return new LogRecord(clock, LogLevel.INFO);
    }

    private static EventLogger logger(LogRecord record, String node) {
        EventLogger logger = new EventLogger(record, node);
        logger.setLevel(LogLevel.INFO);
        return logger;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> nodeProps(Object doc, String node) {
        Map<String, Object> rec = (Map<String, Object>) ((Map<String, Object>) doc).get("eventLogRecord");
        Map<String, Object> merged = new LinkedHashMap<>();
        for (Map<String, Object> entry : (List<Map<String, Object>>) rec.get("nodeLogs")) {
            Object props = entry.get(node);
            if (props != null) {
                merged.putAll((Map<String, Object>) props);
            }
        }
        return merged;
    }
}
