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
            "",
            "true", "false", "True", "yes", "No", "on", "OFF",
            "null", "Null", "~",
            "123", "-5", "+7", "1.5", "1e10", ".5", "0x1F", "1_000", "12:30:00", "2024-01-01",
            ".inf", "-.inf", ".nan", "NaN", "Infinity",
            "-", "- item", "? q", "&anchor", "*alias", "!tag", "|", ">", "%", "@at", "`tick`", "=", "<<",
            "ctrl\u0001char", "del\u007f", "nel\u0085", "unicode \u00e9 \u2713 \u65e5\u672c",
            "MutableOrder(id=1, venue=null)",
            "ok?", "is it ok?", "url?x=1", "?",
            "open(", "close)", "a)b(c", "f(g(x)", "(", ")",
            "a \"b, c", "quote\"inside", "smile \uD83D\uDE00, ok", "\uD83D\uDE00",
            "sep\u2028x", "bom\uFEFF", "x\n    textEncoding: QUOTED_SCALARS"
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
        assertEquals("7", props.get("chDigit"));
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

    static final class Thing {
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
