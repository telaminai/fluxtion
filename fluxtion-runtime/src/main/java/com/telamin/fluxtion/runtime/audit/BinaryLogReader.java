/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

    /**
     * Receives each record, then each of its entries.
     *
     * <p>Both ids and resolved names are passed. A filter should match on the <b>id</b> — an integer
     * compare against a set resolved once when the dictionary entry arrived — and use the name only
     * for output. Matching names per entry costs more than reading the file.
     */
    public interface Visitor {
        /**
         * @return {@code true} to receive this record's entries, {@code false} to skip them —
         * the cheap path a time-range or event-type filter takes
         */
        boolean onRecord(int eventTypeId, String eventType,
                         long eventTime, long logTime, long endTime, int entryCount);

        void onEntry(int nodeId, String node, int keyId, String key, int tag, long rawBits);

        /**
         * A name has been resolved to an id. A filter resolves its patterns here, once per name, and
         * matches on ids thereafter.
         */
        default void onDictionaryEntry(int id, String name) {
        }
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

    /**
     * The largest file that can be handled by one {@code FileChannel.map} call. Not arbitrary:
     * {@link MappedByteBuffer} inherits {@code Buffer}'s {@code int} capacity, so a single mapping
     * cannot address more than {@link Integer#MAX_VALUE} bytes.
     */
    public static final long MAX_SINGLE_MAP = Integer.MAX_VALUE;

    /** Chunk size for the streamed path. Package-visible so a test can force the straddle case. */
    static final int DEFAULT_CHUNK = 1 << 20;

    private BinaryLogReader() {
    }

    /**
     * Reads a log file, memory-mapping it when it fits a single mapping and streaming it when it does
     * not. Both paths produce identical results; the difference is only how the bytes are obtained.
     */
    public static Result read(Path path, Visitor visitor) throws IOException {
        long size = Files.size(path);
        if (size <= MAX_SINGLE_MAP) {
            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
                MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
                byte[] all = new byte[(int) size];
                map.get(all);
                return read(all, visitor);
            }
        }
        try (InputStream in = Files.newInputStream(path)) {
            return readStreamed(in, DEFAULT_CHUNK, visitor);
        }
    }

    /**
     * The streamed path, for files past {@link #MAX_SINGLE_MAP}.
     *
     * <p>A frame can straddle a chunk boundary, so whatever the parser could not consume is carried to
     * the front of the next chunk. Without that a record spanning the boundary would be reported as a
     * truncation in the middle of a perfectly good file — which is why the straddle case is tested with
     * a deliberately small chunk rather than left to a 2 GiB fixture nobody will build.
     *
     * @param chunk bytes to read at a time; a test uses a small value to force straddles
     */
    static Result readStreamed(InputStream in, int chunk, Visitor visitor) throws IOException {
        byte[] buffer = new byte[Math.max(chunk, BinaryLogFile.HEADER_BYTES)];
        int held = 0;
        Result total = new Result();
        boolean first = true;
        List<String> names = total.dictionary;

        while (true) {
            int room = buffer.length - held;
            if (room == 0) {
                // a single frame is larger than the chunk: grow rather than stall
                byte[] bigger = new byte[buffer.length * 2];
                System.arraycopy(buffer, 0, bigger, 0, held);
                buffer = bigger;
                room = buffer.length - held;
            }
            int n = in.read(buffer, held, room);
            if (n < 0) {
                break;
            }
            held += n;
            if (first && held < BinaryLogFile.HEADER_BYTES) {
                // not enough yet to validate the header — read more before parsing anything
                continue;
            }

            byte[] slice = new byte[held];
            System.arraycopy(buffer, 0, slice, 0, held);
            Cursor cursor = parse(slice, first, names, total, visitor);
            first = false;
            int consumed = cursor.consumed;
            System.arraycopy(buffer, consumed, buffer, 0, held - consumed);
            held -= consumed;
        }
        if (first) {
            // the stream ended before a header could be validated — the mapped path throws here and
            // the two paths must agree, or "read a file" means something different depending on size
            throw new IOException("not an audit log: stream ended after " + held
                    + " bytes, need at least " + BinaryLogFile.HEADER_BYTES);
        }
        total.truncatedBytes = held;
        return total;
    }

    public static Result read(byte[] data, Visitor visitor) throws IOException {
        Result result = new Result();
        Cursor c = parse(data, true, result.dictionary, result, visitor);
        result.truncatedBytes = data.length - c.consumed;
        return result;
    }

    /** How far the parser got. */
    private static final class Cursor {
        int consumed;
    }

    /**
     * Parses whole frames from {@code data}, stopping at the first incomplete one.
     *
     * @param expectHeader true for the first slice of a file, false when resuming mid-stream
     * @return how many bytes were consumed; the remainder is an incomplete frame to carry or report
     */
    private static Cursor parse(byte[] data, boolean expectHeader, List<String> names,
                                Result result, Visitor visitor) throws IOException {
        Cursor cursor = new Cursor();
        int p = 0;
        if (expectHeader) {
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
                throw new IOException("audit log format version " + version
                        + ", this reader understands " + BinaryLogFile.FORMAT_VERSION);
            }
            p = BinaryLogFile.HEADER_BYTES;
            cursor.consumed = p;
        }
        while (p < data.length) {
            int frame = data[p] & 0xFF;
            if (frame == BinaryLogFile.FRAME_DICT) {
                if (p + 5 > data.length) { break; }
                int id = u16(data, p + 1);
                int len = u16(data, p + 3);
                if (p + 5 + len > data.length) { break; }
                while (names.size() <= id) { names.add(null); }
                String name = new String(data, p + 5, len, java.nio.charset.StandardCharsets.UTF_8);
                names.set(id, name);
                visitor.onDictionaryEntry(id, name);
                p += 5 + len;
                cursor.consumed = p;
            } else if (frame == BinaryLogFile.FRAME_RECORD) {
                if (p + BinaryLogFile.RECORD_FIXED_BYTES > data.length) { break; }
                int entries = u16(data, p + 1);
                int eventTypeId = u16(data, p + 3);
                long eventTime = i64(data, p + 5);
                long logTime = i64(data, p + 13);
                long endTime = i64(data, p + 21);
                int slotBytes = entries * 16;
                if (p + BinaryLogFile.RECORD_FIXED_BYTES + slotBytes > data.length) { break; }
                int base = p + BinaryLogFile.RECORD_FIXED_BYTES;
                String eventType = name(names, eventTypeId, result);
                result.records++;
                if (visitor.onRecord(eventTypeId, eventType, eventTime, logTime, endTime, entries)) {
                    for (int e = 0; e < entries; e++) {
                        long header = i64(data, base + e * 16);
                        long bits = i64(data, base + e * 16 + 8);
                        int nodeId = BinaryRecordDecoder.nodeId(header);
                        int keyId = BinaryRecordDecoder.keyId(header);
                        visitor.onEntry(nodeId, name(names, nodeId, result),
                                keyId, name(names, keyId, result),
                                BinaryRecordDecoder.tag(header), bits);
                    }
                }
                result.entries += entries;
                p = base + slotBytes;
                cursor.consumed = p;
            } else {
                throw new IOException("unknown frame type 0x" + Integer.toHexString(frame)
                        + " at byte " + p);
            }
        }
        return cursor;
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
