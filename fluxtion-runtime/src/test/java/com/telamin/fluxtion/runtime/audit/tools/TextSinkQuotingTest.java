/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit.tools;

import com.telamin.fluxtion.runtime.audit.BinaryEventLogger;
import com.telamin.fluxtion.runtime.audit.BinaryLogRecord;
import com.telamin.fluxtion.runtime.audit.BinaryLogWriter;
import com.telamin.fluxtion.runtime.audit.BinaryRecordDecoder;
import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;
import com.telamin.fluxtion.runtime.audit.EventLogger;
import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The CLI's YAML output quotes text values the way the text record does, and marks the records that
 * hold a quoted value with the same {@code textEncoding: QUOTED_SCALARS} first line.
 */
public class TextSinkQuotingTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final String NL = System.lineSeparator();

    private static final int STR = BinaryRecordDecoder.TAG_CHARSEQ, OBJ = BinaryRecordDecoder.TAG_OBJECT,
            INT = BinaryRecordDecoder.TAG_INT, BOOL = BinaryRecordDecoder.TAG_BOOL;

    @Test
    public void sinkQuotesTextValuesAndMarksOnlyThoseRecords() {
        // the third record: a CHARSEQ "true" is a String on the wire, so it is quoted and marks the
        // record; an OBJECT "9" (a boxed number's toString) stays bare as the text record writes it
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        AuditLogTool.TextSink sink = new AuditLogTool.TextSink(new PrintStream(bytes, true));
        sink.record("Tick", 1, 2, 3);
        sink.entry("gate", "reason", "not active, due 30.0", STR, 1);
        sink.entry("gate", "count", "3", INT, 3);
        sink.record("Tick", 4, 5, 6);
        sink.entry("gate", "reason", "active", STR, 2);
        sink.entry("gate", "count", "4", INT, 4);
        sink.record("Tick", 7, 8, 9);
        sink.entry("gate", "flag", "true", BOOL, 1);
        sink.entry("gate", "s", "true", STR, 3);
        sink.entry("gate", "o", "9", OBJ, 4);
        sink.close();
        String expected = String.join(NL,
                "eventLogRecord: ",
                "    textEncoding: QUOTED_SCALARS",
                "    eventTime: 1",
                "    logTime: 2",
                "    event: Tick",
                "    nodeLogs: ",
                "        - gate: { reason: \"not active, due 30.0\"}",
                "        - gate: { count: 3}",
                "    endTime: 3",
                "",
                "eventLogRecord: ",
                "    eventTime: 4",
                "    logTime: 5",
                "    event: Tick",
                "    nodeLogs: ",
                "        - gate: { reason: active}",
                "        - gate: { count: 4}",
                "    endTime: 6",
                "",
                "eventLogRecord: ",
                "    textEncoding: QUOTED_SCALARS",
                "    eventTime: 7",
                "    logTime: 8",
                "    event: Tick",
                "    nodeLogs: ",
                "        - gate: { flag: true}",
                "        - gate: { s: \"true\"}",
                "        - gate: { o: 9}",
                "    endTime: 9") + NL;
        assertEquals(expected, bytes.toString());
    }

    @Test
    public void endToEndFromABinaryLog() throws Exception {
        Path log = folder.newFile("audit.flxa").toPath();
        Clock clock = new Clock();
        clock.init();
        BinaryLogRecord record = new BinaryLogRecord(clock, 4096);
        record.updateLogLevel(LogLevel.INFO);
        EventLogger logger = new BinaryEventLogger(record, "gate");
        logger.setLevel(LogLevel.INFO);
        try (OutputStream os = Files.newOutputStream(log); BinaryLogWriter writer = new BinaryLogWriter(os)) {
            record.triggerObject(new Object());
            logger.info("reason", "not active, due 30.0");
            logger.info("n", 3);
            logger.info("c", ',');
            logger.info("nil", (CharSequence) null);
            logger.infoText("op", "472.1");
            logger.info("plainId", "472.10");
            logger.info("boxed", (Object) Integer.valueOf(9));
            record.terminateRecord();
            writer.processLogRecord(record);

            record.triggerObject(new Object());
            logger.info("reason", "active");
            logger.info("n", 4);
            record.terminateRecord();
            writer.processLogRecord(record);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        assertEquals(0, AuditLogTool.run(new String[]{log.toString()}, new PrintStream(out, true),
                new PrintStream(err, true)));
        String text = out.toString();
        String[] records = text.split(NL + NL);
        assertEquals(text, 2, records.length);
        assertTrue(text, records[0].startsWith("eventLogRecord: " + NL + "    textEncoding: QUOTED_SCALARS" + NL));
        assertTrue(text, records[0].contains("{ reason: \"not active, due 30.0\"}"));
        assertTrue(text, records[0].contains("{ n: 3}"));
        assertTrue(text, records[0].contains("{ c: \",\"}"));
        assertTrue(text, records[0].contains("{ nil: null}"));
        // CHARSEQ is a String on the wire however it was logged: both ids stay distinct strings
        assertTrue(text, records[0].contains("{ op: \"472.1\"}"));
        assertTrue(text, records[0].contains("{ plainId: \"472.10\"}"));
        assertTrue(text, records[0].contains("{ boxed: 9}"));
        assertFalse(text, records[1].contains("textEncoding"));
        assertTrue(text, records[1].contains("{ reason: active}"));
    }
}
