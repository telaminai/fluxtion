/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

/**
 * Writes {@link BinaryLogRecord}s to a stream in the {@link BinaryLogFile} framing.
 *
 * <p>Install it as the audit sink and the log becomes readable by {@link BinaryLogReader}:
 * <pre>
 *   eventLogManager.setLogSink(new BinaryLogWriter(Files.newOutputStream(path)));
 * </pre>
 *
 * <p>The record is reused and cleared after publish, so everything needed is taken during the callback
 * — a sink that keeps a reference and reads it later sees an empty record.
 *
 * <p>Dictionary entries are emitted the first time an id appears, so a reader never meets an id it has
 * not been told about, and the file stays self-describing without repeating names per entry.
 */
public final class BinaryLogWriter implements LogRecordListener, Closeable {

    private final OutputStream out;
    private final byte[] scratch = new byte[BinaryLogFile.RECORD_FIXED_BYTES];
    private int dictionaryWritten;
    private long recordsWritten;

    public BinaryLogWriter(OutputStream out) {
        this.out = out;
        try {
            out.write(BinaryLogFile.MAGIC);
            writeShort(BinaryLogFile.FORMAT_VERSION);
            writeShort(0);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write audit log header", e);
        }
    }

    @Override
    public void processLogRecord(LogRecord logRecord) {
        if (!(logRecord instanceof BinaryLogRecord)) {
            throw new IllegalArgumentException("BinaryLogWriter needs a BinaryLogRecord, got "
                    + logRecord.getClass().getName()
                    + " — build with addLowLatencyEventLog(level, AuditRecordFormat.BINARY)");
        }
        BinaryLogRecord record = (BinaryLogRecord) logRecord;
        try {
            emitNewDictionaryEntries(record.dictionary());
            int entries = record.length() / 16;
            out.write(BinaryLogFile.FRAME_RECORD);
            writeShort(entries);
            writeShort(record.eventTypeId());
            writeLong(record.eventTime());
            writeLong(record.logTime());
            writeLong(record.endTime());
            long[] slots = record.slots();
            for (int i = 0; i < entries * 2; i++) {
                writeLong(slots[i]);
            }
            recordsWritten++;
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write audit record", e);
        }
    }

    /** Only ids not yet described are written, so the cost is paid once per name, not per record. */
    private void emitNewDictionaryEntries(String[] dictionary) throws IOException {
        for (int id = dictionaryWritten; id < dictionary.length; id++) {
            String name = dictionary[id];
            if (name == null) {
                continue;
            }
            byte[] utf8 = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            out.write(BinaryLogFile.FRAME_DICT);
            writeShort(id);
            writeShort(utf8.length);
            out.write(utf8);
        }
        dictionaryWritten = Math.max(dictionaryWritten, dictionary.length);
    }

    public long recordsWritten() {
        return recordsWritten;
    }

    private void writeShort(int v) throws IOException {
        out.write((v >>> 8) & 0xFF);
        out.write(v & 0xFF);
    }

    private void writeLong(long v) throws IOException {
        for (int shift = 56; shift >= 0; shift -= 8) {
            out.write((int) (v >>> shift) & 0xFF);
        }
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
