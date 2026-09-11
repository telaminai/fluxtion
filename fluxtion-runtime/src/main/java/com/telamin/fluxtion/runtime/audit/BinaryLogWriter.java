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

    /** The dictionary frame declares a name's byte length in a u16, so this is the hard maximum. */
    private static final int MAX_DICTIONARY_NAME_BYTES = 0xFFFF;

    /** id -> name as actually written to this file, so a conflicting reuse can be refused. */
    private final java.util.Map<Integer, String> namesWritten = new java.util.HashMap<>();

    @Override
    public void processLogRecord(LogRecord logRecord) {
        if (!(logRecord instanceof BinaryLogRecord)) {
            throw new IllegalArgumentException("BinaryLogWriter needs a BinaryLogRecord, got "
                    + logRecord.getClass().getName()
                    + " — build with addLowLatencyEventLog(level, AuditRecordFormat.BINARY)");
        }
        BinaryLogRecord record = (BinaryLogRecord) logRecord;
        // A RECORD THAT OVERFLOWED IS NOT A RECORD. writeSlots sets the flag and drops the entries it
        // could not fit; writing the prefix anyway produced a well-formed file whose records were
        // silently short, so a reader could not tell a complete log from lost audit evidence. Audit
        // output exists to be trusted about what happened, so losing part of it has to be loud.
        if (record.overflowed()) {
            throw new IllegalStateException(
                    "audit record overflowed its buffer - " + (record.length() / 16)
                            + " entries fit and the rest were dropped. Writing it would produce a file "
                            + "that looks complete and is not. Reduce entries logged per event, or raise "
                            + "the record buffer, and re-run.");
        }
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

    /**
     * Only ids not yet described are written, so the cost is paid once per name, not per record.
     *
     * <p><b>This assumes one record instance per writer</b>, whose dictionary only ever grows —
     * which is what the runtime does: {@code EventLogManager} holds a single record and clears it
     * between events, so an id means the same name for the life of the file.
     *
     * <p>Hand a SECOND record instance to the same writer and that assumption breaks silently. The new
     * record's ids restart at 1, every one of them below the high-water mark, so nothing is emitted and
     * the reader goes on resolving those ids to the FIRST record's names — every entry after the first
     * record is attributed to the wrong node, in a file that parses cleanly. Found by a test fixture
     * that built a record per iteration; the check below turns it into a refusal.
     */
    private void emitNewDictionaryEntries(String[] dictionary) throws IOException {
        for (int id = 1; id < Math.min(dictionaryWritten, dictionary.length); id++) {
            String name = dictionary[id];
            if (name != null && !name.equals(namesWritten.get(id))) {
                throw new IllegalStateException(
                        "audit dictionary id " + id + " was written as '" + namesWritten.get(id)
                                + "' and this record calls it '" + name + "'. Ids are file-scoped, so "
                                + "reusing one for a second name would silently re-label every earlier "
                                + "entry. A writer takes ONE record instance, reused across events - "
                                + "the runtime clears and reuses a single record rather than making a "
                                + "new one per event.");
            }
        }
        for (int id = dictionaryWritten; id < dictionary.length; id++) {
            String name = dictionary[id];
            if (name == null) {
                continue;
            }
            byte[] utf8 = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            // The length field is u16. Without this check a longer name wrote a TRUNCATED length
            // followed by every byte, so the reader resumed mid-string and read payload as frame tags -
            // "unknown frame type 0x78" some thousands of bytes later, with nothing pointing at the
            // cause. Reachable from a public API: audit VALUES are interned as dictionary names.
            if (utf8.length > MAX_DICTIONARY_NAME_BYTES) {
                throw new IllegalStateException(
                        "audit dictionary name is " + utf8.length + " UTF-8 bytes; the record format's "
                                + "length field holds at most " + MAX_DICTIONARY_NAME_BYTES
                                + ". Writing it would corrupt the file rather than truncate the name. "
                                + "This is almost always a long String VALUE being logged as an audit "
                                + "entry; log an identifier instead.");
            }
            out.write(BinaryLogFile.FRAME_DICT);
            writeShort(id);
            writeShort(utf8.length);
            out.write(utf8);
            namesWritten.put(id, name);
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
