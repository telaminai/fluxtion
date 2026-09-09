package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;
import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A string-valued audit entry must survive a binary record.
 *
 * <p><b>Every one of them used to vanish.</b> {@code BinaryLogRecord} has two write paths — the
 * two-slot form the reader parses, and a byte-stream form used only by the String-keyed overloads.
 * Booleans and numbers reached the slot path; {@code CharSequence} and {@code Object} had none, so they
 * wrote length-prefixed characters into a buffer {@code length()} does not describe and the reader,
 * which parses slots, never saw them. {@code auditLog.info("mapFunction", auditInfo)} produced NOTHING
 * — not a truncation, not a rendering fault: no entry existed.
 *
 * <p>For the DSL that meant the entries naming WHICH function ran — {@code mapFunction},
 * {@code filterFunction}, {@code pushTarget} — were exactly the ones missing from every binary log.
 *
 * <p>It was found by comparing a Java audit log against the log a C++ target wrote for the same graph,
 * which lives in another repository. This test is here so a change to core alone cannot reintroduce it.
 */
public class StringValuedAuditEntryTest {

    @Test
    public void aStringValueSurvivesTheBinaryRecord() throws Exception {
        Clock clock = new Clock();
        clock.init();
        BinaryLogRecord record = new BinaryLogRecord(clock, 4096);
        record.updateLogLevel(LogLevel.INFO);
        // BinaryEventLogger, and its OWN level set: a logger with no level records nothing,
        // independently of the record's level.
        EventLogger logger = new BinaryEventLogger(record, "mapNode");
        logger.setLevel(LogLevel.INFO);

        record.triggerObject(new Tick());
        logger.info("mapFunction", "Tick->getPrice");
        logger.info("invokeMapFunction", true);
        assertTrue("the record must be publishable", record.terminateRecord());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (BinaryLogWriter writer = new BinaryLogWriter(bytes)) {
            writer.processLogRecord(record);
        }

        Map<Integer, String> names = new HashMap<>();
        List<String> entries = new ArrayList<>();
        BinaryLogReader.read(bytes.toByteArray(), new BinaryLogReader.Visitor() {
            @Override
            public boolean onRecord(int typeId, String eventType, long eventTime, long logTime,
                                    long endTime, int entryCount) {
                return true;
            }

            @Override
            public void onDictionaryEntry(int id, String name) {
                names.put(id, name);
            }

            @Override
            public void onEntry(int nodeId, String node, int keyId, String key, int tag, long bits) {
                // A string value is INTERNED - the slot carries a dictionary id, because a value slot
                // is 64 bits and an audit string is nearly always a constant.
                entries.add(node + "." + key + "=" + (tag == 5 || tag == 6
                        ? names.getOrDefault((int) bits, "#unresolved")
                        : BinaryRecordDecoder.renderValue(tag, bits)));
            }
        });

        assertEquals("both entries must be present - the string one used to be absent entirely",
                2, entries.size());
        assertEquals("mapNode.mapFunction=Tick->getPrice", entries.get(0));
        assertEquals("mapNode.invokeMapFunction=true", entries.get(1));
    }

    static class Tick { }
}
