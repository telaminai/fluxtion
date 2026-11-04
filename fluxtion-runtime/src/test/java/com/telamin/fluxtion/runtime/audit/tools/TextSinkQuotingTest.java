/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit.tools;

import com.telamin.fluxtion.runtime.audit.BinaryEventLogger;
import com.telamin.fluxtion.runtime.audit.BinaryLogRecord;
import com.telamin.fluxtion.runtime.audit.BinaryLogWriter;
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

    @Test
    public void sinkQuotesTextValuesAndMarksOnlyThoseRecords() {
        // the third record's text "true" is typed-looking, not structurally unsafe: bare, unmarked
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        AuditLogTool.TextSink sink = new AuditLogTool.TextSink(new PrintStream(bytes, true));
        sink.record("Tick", 1, 2, 3);
        sink.entry("gate", "reason", "not active, due 30.0", true);
        sink.entry("gate", "count", "3", false);
        sink.record("Tick", 4, 5, 6);
        sink.entry("gate", "reason", "active", true);
        sink.entry("gate", "count", "4", false);
        sink.record("Tick", 7, 8, 9);
        sink.entry("gate", "flag", "true", false);
        sink.entry("gate", "s", "true", true);
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
                "    eventTime: 7",
                "    logTime: 8",
                "    event: Tick",
                "    nodeLogs: ",
                "        - gate: { flag: true}",
                "        - gate: { s: true}",
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
        assertFalse(text, records[1].contains("textEncoding"));
        assertTrue(text, records[1].contains("{ reason: active}"));
    }
}
