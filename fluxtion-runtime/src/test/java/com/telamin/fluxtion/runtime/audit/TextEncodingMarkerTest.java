/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;
import com.telamin.fluxtion.runtime.event.Event;
import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The {@code textEncoding: QUOTED_SCALARS} line: present, as the first line of the record's mapping,
 * exactly when the record wrote at least one double-quoted scalar; absent otherwise, leaving the
 * record byte-for-byte as before. Times are formatted as {@code T} so whole records can be compared.
 */
public class TextEncodingMarkerTest {

    static final String MARKER = "eventLogRecord: \n    textEncoding: QUOTED_SCALARS\n    eventTime: ";

    @Test
    public void unquotedRecordIsByteForByteUnchanged() {
        LogRecord record = newRecord();
        EventLogger gate = logger(record, "gate");
        EventLogger other = logger(record, "other");
        record.triggerObject(new Tick());
        gate.info("reason", "membership active");
        gate.info("count", 3);
        other.info("ok", true);
        other.info("order", "MutableOrder(id=1)");
        record.terminateRecord();
        assertEquals("eventLogRecord: \n"
                + "    eventTime: T\n"
                + "    logTime: T\n"
                + "    groupingId: null\n"
                + "    event: Tick\n"
                + "    nodeLogs: \n"
                + "        - gate: { reason: membership active, count: 3}\n"
                + "        - other: { ok: true, order: MutableOrder(id=1)}\n"
                + "    endTime: T", record.toString());
    }

    @Test
    public void quotedValueAddsMarkerAsFirstLineOnce() {
        LogRecord record = newRecord();
        EventLogger gate = logger(record, "gate");
        EventLogger other = logger(record, "other");
        record.triggerObject(new Tick());
        gate.info("a", "plain");
        gate.info("reason", "not active, balance due 30.0");
        other.info("b", "x: y");
        other.info("c", 7);
        record.terminateRecord();
        assertEquals("eventLogRecord: \n"
                + "    textEncoding: QUOTED_SCALARS\n"
                + "    eventTime: T\n"
                + "    logTime: T\n"
                + "    groupingId: null\n"
                + "    event: Tick\n"
                + "    nodeLogs: \n"
                + "        - gate: { a: plain, reason: \"not active, balance due 30.0\"}\n"
                + "        - other: { b: \"x: y\", c: 7}\n"
                + "    endTime: T", record.toString());
    }

    @Test
    public void quotedValueInLaterNodeOnlyStillMarks() {
        LogRecord record = newRecord();
        record.triggerObject(new Tick());
        logger(record, "n1").info("v", "plain");
        logger(record, "n2").info("v", 1.5);
        logger(record, "n3").info("v", "ok?");
        record.terminateRecord();
        assertMarked(record.toString());
    }

    @Test
    public void everyTextPathMarks() {
        Object[] values = {',', (Object) Character.valueOf('#'), new StringBuilder("a, b"), "- item",
                new Thing("x, y"), "open(", "lone \uD800"};
        for (Object v : values) {
            LogRecord record = newRecord();
            record.triggerObject(new Tick());
            EventLogger logger = logger(record, "gate");
            if (v instanceof Character) {
                logger.info("v", (char) (Character) v);
            } else if (v instanceof CharSequence) {
                logger.info("v", (CharSequence) v);
            } else {
                logger.info("v", v);
            }
            record.terminateRecord();
            assertMarked(record.toString());
        }
    }

    @Test
    public void numbersBooleansAndNullsNeverMark() {
        LogRecord record = newRecord();
        record.triggerObject(new Tick());
        EventLogger logger = logger(record, "gate");
        logger.info("i", 1);
        logger.info("d", -2.5);
        logger.info("b", false);
        logger.info("boxed", (Object) Integer.valueOf(9));
        logger.info("n", (Object) null);
        logger.info("s", (CharSequence) null);
        logger.info("c", 'a');
        record.terminateRecord();
        assertUnmarked(record.toString());
    }

    @Test
    public void stateResetsPerRecordWithClear() {
        LogRecord record = newRecord();
        EventLogger logger = logger(record, "gate");

        record.triggerObject(new Tick());
        logger.info("v", "a, b");
        record.terminateRecord();
        assertMarked(record.toString());
        record.clear();

        record.triggerObject(new Tick());
        logger.info("v", "plain");
        record.terminateRecord();
        assertUnmarked(record.toString());
        record.clear();

        record.triggerObject(new Tick());
        logger.info("v", "{again}");
        record.terminateRecord();
        assertMarked(record.toString());
    }

    @Test
    public void stateResetsPerRecordInAnAccumulatingBuffer() {
        // EventLogManager.setClearAfterPublish(false): records accumulate in one buffer, so the marker
        // must go after THIS record's key, not the buffer's first one.
        LogRecord record = newRecord();
        EventLogger logger = logger(record, "gate");
        record.triggerObject(new Tick());
        logger.info("v", "a, b");
        record.terminateRecord();
        int first = record.toString().length();
        record.triggerObject(new Tick());
        logger.info("v", "plain");
        record.terminateRecord();
        int second = record.toString().length();
        record.triggerObject(new Tick());
        logger.info("v", "c: d");
        record.terminateRecord();

        String all = record.toString();
        assertMarked(all.substring(0, first));
        assertUnmarked(all.substring(first, second));
        assertMarked(all.substring(second));
        assertEquals(2, count(all, "textEncoding"));
    }

    @Test
    public void markerPlacedEvenWithoutEndTime() {
        LogRecord record = newRecord();
        record.setRecordEndTime(false);
        record.triggerObject(new Tick());
        logger(record, "gate").info("v", "a, b");
        record.terminateRecord();
        assertMarked(record.toString());
        assertFalse(record.toString().contains("endTime"));
    }

    @Test
    public void terminatingTwiceDoesNotInsertTwice() {
        LogRecord record = newRecord();
        record.triggerObject(new Tick());
        logger(record, "gate").info("v", "a, b");
        record.terminateRecord();
        record.terminateRecord();
        assertEquals(1, count(record.toString(), "textEncoding"));
    }

    @Test
    public void quotedHeaderFieldsMarkAndCannotBreakTheRecord() {
        LogRecord record = newRecord();
        record.groupingId = "grp: 1";
        record.printEventToString(true);
        record.triggerEvent(new Filtered("x\n    textEncoding: QUOTED_SCALARS", "a #b"));
        logger(record, "gate").info("v", 1);
        record.terminateRecord();
        String text = record.toString();
        assertMarked(text);
        assertEquals(1, count(text, "\n    textEncoding"));
        assertTrue(text, text.contains("\n    groupingId: \"grp: 1\"\n"));
        assertTrue(text, text.contains("\n    eventToString: \"x\\n    textEncoding: QUOTED_SCALARS\"\n"));
        assertTrue(text, text.contains("\n    eventFilter: \"a #b\"\n"));
    }

    @Test
    public void plainHeaderFieldsUnchanged() {
        LogRecord record = newRecord();
        record.groupingId = "grp1";
        record.printEventToString(true);
        record.triggerEvent(new Filtered("Filtered-x", "key1"));
        logger(record, "gate").info("v", 1);
        record.terminateRecord();
        String text = record.toString();
        assertUnmarked(text);
        assertTrue(text, text.contains("\n    groupingId: grp1\n    event: Filtered\n"
                + "    eventToString: Filtered-x\n    eventFilter: key1\n"));
    }

    @Test
    public void disabledLoggingWritesNothing() {
        LogRecord record = new LogRecord(clock(), LogLevel.NONE);
        record.triggerObject(new Tick());
        logger(record, "gate").info("v", "a, b");
        record.terminateRecord();
        assertFalse(record.toString().contains("textEncoding"));
    }

    @Test
    public void replacedRecordKeepsMarkerState() {
        LogRecord first = newRecord();
        first.triggerObject(new Tick());
        logger(first, "gate").info("v", "a, b");
        LogRecord second = newRecord();
        second.replaceBuffer(first.asCharSequence());
        second.adoptRecordState(first);
        second.terminateRecord();
        assertMarked(second.toString());
    }

    @Test
    public void typicalToStringHeadersStayPlainAndUnmarked() {
        String[] plain = {"Trade[id=1, px=2.0]", "X(a=1, b=2)", "OrderEvent{id='o1', qty=100}", "a:b", "x#y",
                "k=v, [list], {map}", "unbalanced(", "ok?", "MyEvent"};
        for (String v : plain) {
            LogRecord record = newRecord();
            record.printEventToString(true);
            record.triggerEvent(new Filtered(v, null));
            logger(record, "gate").info("n", 1);
            record.terminateRecord();
            String text = record.toString();
            assertUnmarked(text);
            assertTrue(text, text.contains("\n    eventToString: " + v + "\n"));
        }
    }

    @Test
    public void headerValuesThatWouldBreakTheLineAreQuoted() {
        String[] quoted = {"a: b", "x #y", "ends:", " lead", "trail ", "\u3000wide", "nbsp\u00a0",
                "[leading", "{leading", "- dash", "\"q", "two\nlines", "tab\tx", "lone \uD800"};
        for (String v : quoted) {
            LogRecord record = newRecord();
            record.printEventToString(true);
            record.triggerEvent(new Filtered(v, null));
            logger(record, "gate").info("n", 1);
            record.terminateRecord();
            String text = record.toString();
            assertMarked(text);
            assertTrue(text, text.contains("\n    eventToString: \""));
        }
    }

    /**
     * Typed-looking strings are written bare and the record is unmarked: byte-identical to what runtime
     * 1.1.3 wrote, which appended every value as-is.
     */
    @Test
    public void typedLookingStringsAreByteIdenticalTo113() {
        String[] typed = {"5000", "123", "true", "null", "1e3", "0x1F", "-5", "~", "", "2024-01-01", "No"};
        LogRecord record = newRecord();
        record.groupingId = "42";
        record.printEventToString(true);
        record.triggerEvent(new Filtered("12:30:00", "true"));
        EventLogger logger = logger(record, "gate");
        StringBuilder expected = new StringBuilder("eventLogRecord: \n"
                + "    eventTime: T\n"
                + "    logTime: T\n"
                + "    groupingId: 42\n"
                + "    event: Filtered\n"
                + "    eventToString: 12:30:00\n"
                + "    eventFilter: true\n"
                + "    nodeLogs: \n"
                + "        - gate: {");
        for (int i = 0; i < typed.length; i++) {
            logger.info("v" + i, typed[i]);
            expected.append(i == 0 ? "" : ",").append(" v").append(i).append(": ").append(typed[i]);
        }
        record.terminateRecord();
        expected.append("}\n    endTime: T");
        assertEquals(expected.toString(), record.toString());
        assertUnmarked(record.toString());
    }

    @Test
    public void anonymousEventClassKeepsBareEventField() {
        LogRecord record = newRecord();
        record.triggerObject(new Object() { });
        logger(record, "gate").info("n", 1);
        record.terminateRecord();
        String text = record.toString();
        assertUnmarked(text);
        assertTrue(text, text.contains("\n    event: \n    nodeLogs: "));
    }

    /** A subclass writing its own header, without calling markRecordStart. */
    static final class OwnHeaderRecord extends LogRecord {
        final boolean mark;

        OwnHeaderRecord(boolean mark) {
            super(clock(), LogLevel.INFO);
            this.mark = mark;
        }

        @Override
        public void triggerObject(Object event) {
            if (mark) {
                markRecordStart();
            }
            sb.append("eventLogRecord: \n    custom: yes-ish\n    nodeLogs: ");
        }
    }

    @Test
    public void subclassHeaderIsMarkedWithAndWithoutMarkRecordStart() {
        for (boolean mark : new boolean[]{false, true}) {
            OwnHeaderRecord record = new OwnHeaderRecord(mark);
            EventLogger logger = logger(record, "gate");
            record.triggerObject(new Tick());
            logger.info("v", "plain");
            record.terminateRecord();
            assertFalse(record.toString(), record.toString().contains("textEncoding"));
            record.clear();
            record.triggerObject(new Tick());
            // the key text inside a value must not be taken for the record start
            logger.info("v", "x\neventLogRecord: y");
            logger.info("w", "eventLogRecord: z");
            record.terminateRecord();
            String text = record.toString();
            assertTrue(text, text.startsWith("eventLogRecord: \n    textEncoding: QUOTED_SCALARS\n"
                    + "    custom: yes-ish\n"));
            assertEquals(text, 1, count(text, "textEncoding"));
        }
    }

    static final class AuditedNode extends EventLogNode {
        void log(String k, Object v) {
            auditLog.info(k, v);
        }
    }

    @Test
    public void recordSwapMidRecordKeepsOpenEntryAndMarker() {
        Clock clock = clock();
        java.util.List<String> out = new java.util.ArrayList<>();
        EventLogManager manager = new EventLogManager();
        manager.clock = clock;
        manager.setLogSink(r -> out.add(r.asCharSequence().toString()));
        manager.init();
        AuditedNode node = new AuditedNode();
        manager.nodeRegistered(node, "a");

        Object e = new Object();
        clock.eventReceived(e);
        manager.eventReceived(e);
        node.log("before", "p, q");
        LogRecord replacement = new LogRecord(clock);
        manager.calculationLogConfig(new EventLogControlEvent(replacement));
        node.log("after", 2);
        manager.processingComplete();

        assertEquals(1, out.size());
        String text = out.get(0);
        assertMarked(text);
        assertTrue(text, text.contains("\n        - a: { before: \"p, q\", after: 2}\n"));
        assertEquals(text, 1, count(text, "- a:"));
    }

    private static void assertMarked(String text) {
        assertTrue(text, text.startsWith(MARKER));
        assertEquals(text, 1, count(text, "\n    textEncoding: QUOTED_SCALARS\n"));
    }

    private static void assertUnmarked(String text) {
        assertTrue(text, text.startsWith("eventLogRecord: \n    eventTime: "));
        assertFalse(text, text.contains("textEncoding"));
    }

    private static int count(String s, String sub) {
        int n = 0;
        for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + 1)) {
            n++;
        }
        return n;
    }

    private static Clock clock() {
        Clock clock = new Clock();
        clock.init();
        return clock;
    }

    private static LogRecord newRecord() {
        LogRecord record = new LogRecord(clock(), LogLevel.INFO);
        record.setTimeFormatter((sb, t) -> sb.append('T'));
        return record;
    }

    private static EventLogger logger(LogRecord record, String node) {
        EventLogger logger = new EventLogger(record, node);
        logger.setLevel(LogLevel.INFO);
        return logger;
    }

    static final class Tick {
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

    static final class Filtered implements Event {
        final String text;
        final String filter;

        Filtered(String text, String filter) {
            this.text = text;
            this.filter = filter;
        }

        @Override
        public String filterString() {
            return filter;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
