/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;
import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * {@code infoText} and friends: a value the producer declares to be text is written to read back as a
 * String even when it looks like a number, boolean or null, while {@code info} keeps the 1.1.3 rule.
 */
public class TextValueApiTest {

    static final String[] TYPED = {"472.1", "472.10", "true", "null", "", "5000"};

    @Test
    public void textApiQuotesTypedLookingValuesAndMarks() {
        LogRecord record = newRecord();
        EventLogger logger = logger(record, "ctl", LogLevel.TRACE);
        record.triggerObject(new Object());
        for (int i = 0; i < TYPED.length; i++) {
            logger.infoText("v" + i, TYPED[i]);
        }
        record.terminateRecord();
        String text = record.toString();
        assertTrue(text, text.startsWith("eventLogRecord: \n    textEncoding: QUOTED_SCALARS\n"));
        assertTrue(text, text.contains("{ v0: \"472.1\", v1: \"472.10\", v2: \"true\", v3: \"null\", v4: \"\","
                + " v5: \"5000\"}"));
    }

    @Test
    public void ordinaryInfoStaysBareAndUnmarked() {
        LogRecord record = newRecord();
        EventLogger logger = logger(record, "ctl", LogLevel.TRACE);
        record.triggerObject(new Object());
        for (int i = 0; i < TYPED.length; i++) {
            logger.info("v" + i, TYPED[i]);
        }
        record.terminateRecord();
        String text = record.toString();
        assertFalse(text, text.contains("textEncoding"));
        assertTrue(text, text.contains("{ v0: 472.1, v1: 472.10, v2: true, v3: null, v4: , v5: 5000}"));
    }

    @Test
    public void plainTextStaysPlainAndUnmarked() {
        LogRecord record = newRecord();
        record.triggerObject(new Object());
        logger(record, "ctl", LogLevel.TRACE).infoText("op", "op-472").infoText("nil", null);
        record.terminateRecord();
        String text = record.toString();
        assertFalse(text, text.contains("textEncoding"));
        assertTrue(text, text.contains("{ op: op-472, nil: null}"));
    }

    @Test
    public void everyLevelVariantHonoursTheLevel() {
        LogRecord record = newRecord();
        EventLogger logger = logger(record, "ctl", LogLevel.INFO);
        record.triggerObject(new Object());
        logger.errorText("e", "1").warnText("w", "2").infoText("i", "3").debugText("d", "4").traceText("t", "5");
        logger.logText("l", "6", LogLevel.WARN);
        record.terminateRecord();
        String text = record.toString();
        assertTrue(text, text.contains("{ e: \"1\", w: \"2\", i: \"3\", l: \"6\"}"));
        assertFalse(text, text.contains(" d: ") || text.contains(" t: "));
    }

    @Test
    public void nullLoggerIgnoresText() {
        assertNull(NullEventLogger.INSTANCE.logText("k", "1", LogLevel.INFO));
        NullEventLogger.INSTANCE.infoText("k", "1");
    }

    @Test
    public void apiIsDetectableByReflection() throws Exception {
        assertEquals(EventLogger.class,
                EventLogger.class.getMethod("infoText", String.class, CharSequence.class).getReturnType());
        EventLogger.class.getMethod("logText", String.class, CharSequence.class, LogLevel.class);
    }

    @Test
    public void binaryRecordKeepsTextAsCharSequence() throws Exception {
        Clock clock = clock();
        BinaryLogRecord record = new BinaryLogRecord(clock, 4096);
        record.updateLogLevel(LogLevel.INFO);
        EventLogger logger = new BinaryEventLogger(record, "ctl");
        logger.setLevel(LogLevel.INFO);
        record.triggerObject(new Object());
        logger.infoText("op", "472.1");
        record.terminateRecord();
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (BinaryLogWriter writer = new BinaryLogWriter(bytes)) {
            writer.processLogRecord(record);
        }
        java.util.Map<Integer, String> names = new java.util.HashMap<>();
        java.util.List<String> seen = new java.util.ArrayList<>();
        BinaryLogReader.read(bytes.toByteArray(), new BinaryLogReader.Visitor() {
            @Override
            public boolean onRecord(int typeId, String eventType, long eventTime, long logTime, long endTime,
                                    int entryCount) {
                return true;
            }

            @Override
            public void onDictionaryEntry(int id, String name) {
                names.put(id, name);
            }

            @Override
            public void onEntry(int nodeId, String node, int keyId, String key, int tag, long bits) {
                seen.add(key + "=" + tag + ":" + names.get((int) bits));
            }
        });
        assertEquals("[op=" + BinaryRecordDecoder.TAG_CHARSEQ + ":472.1]", seen.toString());
    }

    private static Clock clock() {
        Clock clock = new Clock();
        clock.init();
        return clock;
    }

    private static LogRecord newRecord() {
        return new LogRecord(clock(), LogLevel.TRACE);
    }

    private static EventLogger logger(LogRecord record, String node, LogLevel level) {
        EventLogger logger = new EventLogger(record, node);
        logger.setLevel(level);
        return logger;
    }
}
