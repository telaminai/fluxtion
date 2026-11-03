/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.builder.replay;

import com.telamin.fluxtion.runtime.audit.LogRecord;
import com.telamin.fluxtion.runtime.audit.LogRecordListener;
import com.telamin.fluxtion.runtime.audit.StreamHeaderRecord;
import com.telamin.fluxtion.runtime.audit.StructuredLogRecord;
import org.yaml.snakeyaml.Yaml;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility for reading {@link LogRecord} yaml and converting to a
 * {@link StructuredLogRecord}
 *
 * @author 2024 gregory higgins.
 */
public class YamlLogRecordListener implements LogRecordListener {

    private final List<StructuredLogRecord> eventList = new ArrayList<>();
    private final List<String> streamHeaders = new ArrayList<>();
    private final Yaml yaml;

    public List<StructuredLogRecord> getEventList() {
        return eventList;
    }

    public YamlLogRecordListener() {
        yaml = new Yaml();
    }

    public void loadFromFile(Reader reader) {
        yaml.loadAll(reader).forEach((Object m) -> {
            if (m != null) {
                Map e = (Map) ((Map) m).get("eventLogRecord");
                if (e != null) {
                    eventList.add(new StructuredLogRecord(e));
                }
            }
        });
    }

    /** The stream headers seen, as their text documents, in arrival order. They are not records. */
    public List<String> getStreamHeaders() {
        return streamHeaders;
    }

    /** A header is stream metadata: kept apart, never parsed into the record list. */
    @Override
    public void processStreamHeader(StreamHeaderRecord header) {
        streamHeaders.add(header.asCharSequence().toString());
    }

    @Override
    public void processLogRecord(LogRecord logRecord) {
        if (logRecord instanceof StreamHeaderRecord) {
            processStreamHeader((StreamHeaderRecord) logRecord);
            return;
        }
        yaml.loadAll(logRecord.asCharSequence().toString()).forEach(m -> {
            if (m instanceof Map) {
                Map e = (Map) ((Map) m).get("eventLogRecord");
                if (e != null) {
                    eventList.add(new StructuredLogRecord(e));
                }
            }
        });
    }

    @Override
    public String toString() {
        return "MarshallingLogRecordListener{" + "eventList=" + eventList + '}';
    }

}
