/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a {@link BinaryLogFile} back — the other half of {@link BinaryLogWriter}, without which a
 * binary audit log is a file nothing can open.
 *
 * <p>It is driven with a {@link Visitor} and hands back primitives plus resolved names, so a filter can
 * reject a record without materialising anything for it.
 *
 * <h2>It does not throw on a damaged tail</h2>
 * The common reason to read an audit log is that a process died, so the file is exactly as complete as
 * it managed to be. A partial trailing frame stops the read cleanly and is reported through
 * {@link Result#truncatedBytes}. An id with no dictionary entry renders as {@code #id} and is counted
 * in {@link Result#unresolvedIds} rather than failing the read — a rolled file may legitimately start
 * mid-stream.
 */
public final class BinaryLogReader {

    /** Receives each record, then each of its entries. */
    public interface Visitor {
        /**
         * @return {@code true} to receive this record's entries, {@code false} to skip them —
         * the cheap path a time-range or event-type filter takes
         */
        boolean onRecord(String eventType, long eventTime, long logTime, long endTime, int entryCount);

        void onEntry(String node, String key, int tag, long rawBits);
    }

    /** What the read found, including what it could not use. */
    public static final class Result {
        public long records;
        public long entries;
        public int truncatedBytes;
        public int unresolvedIds;
        public final List<String> dictionary = new ArrayList<>();

        @Override
        public String toString() {
            return "records=" + records + " entries=" + entries
                    + " truncatedBytes=" + truncatedBytes + " unresolvedIds=" + unresolvedIds;
        }
    }

    private BinaryLogReader() {
    }

    public static Result read(byte[] data, Visitor visitor) throws IOException {
        Result result = new Result();
        if (data.length < BinaryLogFile.HEADER_BYTES) {
            throw new IOException("not an audit log: " + data.length + " bytes, need at least "
                    + BinaryLogFile.HEADER_BYTES);
        }
        for (int i = 0; i < BinaryLogFile.MAGIC.length; i++) {
            if (data[i] != BinaryLogFile.MAGIC[i]) {
                throw new IOException("not an audit log: bad magic");
            }
        }
        int version = u16(data, 4);
        if (version != BinaryLogFile.FORMAT_VERSION) {
            throw new IOException("audit log format version " + version + ", this reader understands "
                    + BinaryLogFile.FORMAT_VERSION);
        }

        List<String> names = result.dictionary;
        int p = BinaryLogFile.HEADER_BYTES;
        while (p < data.length) {
            int frame = data[p] & 0xFF;
            if (frame == BinaryLogFile.FRAME_DICT) {
                if (p + 5 > data.length) { result.truncatedBytes = data.length - p; break; }
                int id = u16(data, p + 1);
                int len = u16(data, p + 3);
                if (p + 5 + len > data.length) { result.truncatedBytes = data.length - p; break; }
                while (names.size() <= id) { names.add(null); }
                names.set(id, new String(data, p + 5, len, java.nio.charset.StandardCharsets.UTF_8));
                p += 5 + len;
            } else if (frame == BinaryLogFile.FRAME_RECORD) {
                if (p + BinaryLogFile.RECORD_FIXED_BYTES > data.length) {
                    result.truncatedBytes = data.length - p;
                    break;
                }
                int entries = u16(data, p + 1);
                int eventTypeId = u16(data, p + 3);
                long eventTime = i64(data, p + 5);
                long logTime = i64(data, p + 13);
                long endTime = i64(data, p + 21);
                int slotBytes = entries * 16;
                if (p + BinaryLogFile.RECORD_FIXED_BYTES + slotBytes > data.length) {
                    result.truncatedBytes = data.length - p;
                    break;
                }
                int base = p + BinaryLogFile.RECORD_FIXED_BYTES;
                String eventType = name(names, eventTypeId, result);
                result.records++;
                if (visitor.onRecord(eventType, eventTime, logTime, endTime, entries)) {
                    for (int e = 0; e < entries; e++) {
                        long header = i64(data, base + e * 16);
                        long bits = i64(data, base + e * 16 + 8);
                        visitor.onEntry(
                                name(names, BinaryRecordDecoder.nodeId(header), result),
                                name(names, BinaryRecordDecoder.keyId(header), result),
                                BinaryRecordDecoder.tag(header), bits);
                    }
                }
                result.entries += entries;
                p = base + slotBytes;
            } else {
                throw new IOException("unknown frame type 0x" + Integer.toHexString(frame)
                        + " at byte " + p);
            }
        }
        return result;
    }

    /** An id with no dictionary entry renders as {@code #id} and is counted, never thrown. */
    private static String name(List<String> names, int id, Result result) {
        if (id >= 0 && id < names.size() && names.get(id) != null) {
            return names.get(id);
        }
        result.unresolvedIds++;
        return "#" + id;
    }

    private static int u16(byte[] d, int p) {
        return ((d[p] & 0xFF) << 8) | (d[p + 1] & 0xFF);
    }

    private static long i64(byte[] d, int p) {
        long v = 0;
        for (int i = 0; i < 8; i++) { v = (v << 8) | (d[p + i] & 0xFF); }
        return v;
    }
}
