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
 *
 * <p><b>What a refusal guarantees.</b> Every SEMANTIC refusal - an overflowed record, too many entries
 * for the count field, a name too long for its length field, more new names than the file has ids
 * left - is decided before the first byte of the record is written, so the stream and the writer's
 * dictionary are exactly as they were. That is validation, not transaction: an {@link IOException}
 * from the stream mid-frame leaves a partial frame, which a reader reports as an unreadable tail.
 * Two limits are easy to confuse: this writer's FILE dictionary holds 65,535 names; a
 * {@link BinaryLogRecord}'s own intern table holds 32,767, so one record can never exhaust the file
 * alone, but several record instances can.
 */
public final class BinaryLogWriter implements LogRecordListener, Closeable {

    private final OutputStream out;
    private final byte[] scratch = new byte[BinaryLogFile.RECORD_FIXED_BYTES];
    private int dictionaryWritten;
    private long recordsWritten;

    private final int timeUnit;

    /** Writes epoch-millisecond timestamps — Java's default clock — and says so in the header. */
    public BinaryLogWriter(OutputStream out) {
        this(out, BinaryLogFile.TIME_UNIT_EPOCH_MILLIS);
    }

    /**
     * @param timeUnit one of the {@code BinaryLogFile.TIME_UNIT_*} codes. The writer cannot know the
     *                 installed clock's unit - a strategy is a bare {@code long} supplier by design -
     *                 so whoever installs a non-default strategy states the unit here, and the file
     *                 carries it to every reader. Before this field existed the analyser labelled every
     *                 file milliseconds while the C++ runtime wrote nanoseconds into the same fields.
     *                 The unit is stated ONCE, here, for the whole file: the clock strategy is a
     *                 process-wide singleton chosen before the processor is built, and changing it
     *                 while a writer is open produces a file whose header is wrong for every record
     *                 after the change. Start a new writer with the new unit instead.
     *                 See {@link BinaryLogFile#TIME_UNIT_EPOCH_MILLIS} for which fields the unit
     *                 governs - an {@link com.telamin.fluxtion.runtime.event.Event}'s own time is
     *                 not one of them.
     * @throws IllegalArgumentException for a code the format does not define; nothing is written
     */
    public BinaryLogWriter(OutputStream out, int timeUnit) {
        // Before the header. The field is a u16: 65,537 wrapped to 1 and the file claimed
        // milliseconds; 3 was written as 3 and a reader took it for whatever it liked.
        BinaryLogFile.checkTimeUnit(timeUnit);
        this.timeUnit = timeUnit;
        this.out = out;
        try {
            out.write(BinaryLogFile.MAGIC);
            writeShort(BinaryLogFile.FORMAT_VERSION);
            writeShort(timeUnit);   // was the reserved u16, always 0; 0 still means "unspecified"
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write audit log header", e);
        }
    }


    /** The file's dictionary: name -> the id this FILE uses for it. Authoritative over any record. */
    private final java.util.Map<String, Integer> fileIdByName = new java.util.HashMap<>();
    private int nextFileId = 1;

    /** Cache so the common case - one record instance, reused - does no per-record work. */
    private BinaryLogRecord lastRecord;
    private int lastDictionaryLength = -1;
    private int[] translation = new int[0];


    /** Slot 0 packs nodeId in bits 48-63 and keyId in 32-47; both are record-scoped ids. */
    private static long translateEntry(long slot0, int[] translate) {
        long node = mapped(slot0 >>> 48, translate);
        long key = mapped((slot0 >>> 32) & 0xFFFFL, translate);
        return (node << 48) | ((key & 0xFFFFL) << 32) | (slot0 & 0xFFFFFFFFL);
    }

    /** Id 0 is "none" and stays 0; an id the record never described passes through unchanged. */
    private static long mapped(long id, int[] translate) {
        return id > 0 && id < translate.length && translate[(int) id] != 0 ? translate[(int) id] : id;
    }

    private static final long TAG_CHARSEQ = 5, TAG_OBJECT = 6;

    @Override
    public void processLogRecord(LogRecord logRecord) {
        if (!(logRecord instanceof BinaryLogRecord)) {
            throw new IllegalArgumentException("BinaryLogWriter needs a BinaryLogRecord, got "
                    + logRecord.getClass().getName()
                    + " — build with addLowLatencyEventLog(level, AuditRecordFormat.BINARY)");
        }
        BinaryLogRecord record = (BinaryLogRecord) logRecord;
        // ONE representability rule, owned by the record and shared with encodeTo(). An overflowed
        // record is not a record (its tail was dropped), and more entries than the u16 count field
        // holds would wrap the count and corrupt the file. Both are refused before any RECORD byte -
        // and before any DICTIONARY byte, so a refused record leaves the file exactly as it was.
        record.checkEncodable();
        try {
            int[] translate = translationFor(record);
            int entries = record.length() / 16;
            out.write(BinaryLogFile.FRAME_RECORD);
            writeShort(entries);
            // TRANSLATED. eventTypeId is allocated by the record's own tableId(), so it is exactly as
            // record-scoped as the node, key and value ids below - and it was the one id left out. A
            // replacement record allocated B into the id A held in the first record, and the file went
            // on calling that id A: a B event silently recorded as A, integrity counter clean. The
            // highest-risk polarity an audit system has.
            writeShort((int) mapped(record.eventTypeId(), translate));
            writeLong(record.eventTime());
            writeLong(record.logTime());
            writeLong(record.endTime());
            long[] slots = record.slots();
            for (int i = 0; i < entries * 2; i += 2) {
                writeLong(translateEntry(slots[i], translate));
                // A CharSequence/Object VALUE is a dictionary id too, so it needs the same mapping.
                long tag = slots[i] & 0xFFL;
                writeLong(tag == TAG_CHARSEQ || tag == TAG_OBJECT
                        ? mapped(slots[i + 1], translate)
                        : slots[i + 1]);
            }
            recordsWritten++;
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write audit record", e);
        }
    }

    /**
     * Maps this record's ids onto the FILE's ids, emitting any name the file has not described yet.
     *
     * <p><b>Why a translation and not a high-water mark.</b> A dictionary id is scoped to the RECORD
     * that allocated it; the file's dictionary is scoped to the writer. Those coincide only while one
     * record instance is reused, which is what the runtime normally does — but not what the API
     * promises: {@code EventLogControlEvent} can replace the {@code LogRecord} at runtime without
     * replacing the sink, and {@code EventLogManager} documents that swap as supported. On replacement
     * the new record re-interns names by iterating a {@code HashMap}, so the same graph can allocate
     * the same ids to different names.
     *
     * <p>Assuming the mark was enough produced a cleanly-parsed file with every entry after the swap
     * attributed to the wrong node. Refusing the swap instead, as an earlier fix did, was honest but
     * rejected a published contract. Translating honours it: names are the identity, ids are an
     * encoding detail of whoever allocated them.
     *
     * <p>The common case costs nothing — same record instance, unchanged dictionary, cached map.
     */
    private int[] translationFor(BinaryLogRecord record) throws IOException {
        String[] dictionary = record.dictionary();
        if (record == lastRecord && dictionary.length == lastDictionaryLength) {
            return translation;
        }
        // PREFLIGHT, then emit. A review found the oversize-name refusal firing AFTER earlier names
        // of the same record had been written as DICT frames: the stream was well-formed but not
        // unchanged, and the writer's id table had advanced for a record that was never written. So
        // every semantic refusal - a name too long for its length field, more new names than ids
        // remain - is decided over the whole record before the first byte of it.
        int newNames = 0;
        for (int id = 1; id < dictionary.length; id++) {
            String name = dictionary[id];
            if (name == null || fileIdByName.containsKey(name)) {
                continue;
            }
            checkNameLength(name);
            newNames++;
        }
        if (nextFileId - 1 + newNames > BinaryLogFile.MAX_DICTIONARY_ID) {
            throw new IllegalStateException(
                    "audit file dictionary would overflow: " + (nextFileId - 1) + " names defined, "
                            + newNames + " new in this record, and the record format's u16 id holds "
                            + BinaryLogFile.MAX_DICTIONARY_ID + ". Nothing was written. Start a new "
                            + "file, or log unbounded values as an identifier rather than as a "
                            + "distinct string each time.");
        }
        int[] map = new int[dictionary.length];
        for (int id = 1; id < dictionary.length; id++) {
            String name = dictionary[id];
            if (name == null) {
                continue;
            }
            Integer fileId = fileIdByName.get(name);
            if (fileId == null) {
                fileId = nextFileId++;
                emitDictionaryEntry(fileId, name);
            }
            map[id] = fileId;
        }
        lastRecord = record;
        lastDictionaryLength = dictionary.length;
        translation = map;
        return map;
    }

    /**
     * The length field is u16. Without this check a longer name wrote a TRUNCATED length followed by
     * every byte, so the reader resumed mid-string and read payload as frame tags. Reachable from a
     * public API: audit VALUES are interned as dictionary names. Run in the preflight, before any byte.
     */
    private static void checkNameLength(String name) {
        int utf8Length = utf8Length(name);
        if (utf8Length > BinaryLogFile.MAX_DICTIONARY_NAME_BYTES) {
            throw new IllegalStateException(
                    "audit dictionary name is " + utf8Length + " UTF-8 bytes; the record format's "
                            + "length field holds at most " + BinaryLogFile.MAX_DICTIONARY_NAME_BYTES
                            + ". Writing it would corrupt the file rather than truncate the name. "
                            + "Nothing was written. This is almost always a long String VALUE being "
                            + "logged as an audit entry; log an identifier instead.");
        }
    }

    /** UTF-8 length without allocating the bytes - the preflight runs once per NEW name only. */
    private static int utf8Length(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x80) n += 1;
            else if (c < 0x800) n += 2;
            else if (Character.isHighSurrogate(c) && i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1))) { n += 4; i++; }
            else n += 3;
        }
        return n;
    }

    private void emitDictionaryEntry(int id, String name) throws IOException {
        byte[] utf8 = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        out.write(BinaryLogFile.FRAME_DICT);
        writeShort(id);
        writeShort(utf8.length);
        out.write(utf8);
        fileIdByName.put(name, id);
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
