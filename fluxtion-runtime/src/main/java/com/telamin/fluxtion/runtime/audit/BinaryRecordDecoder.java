/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

/**
 * Reads back what {@link BinaryLogRecord} writes.
 *
 * <p>A binary audit log nothing can open is not a feature, so the decoder ships beside the encoder and
 * is tested against it round-trip. It is deliberately allocation-free on the read path: a caller drives
 * it with a {@link Visitor} and gets primitives, so a reader can filter millions of entries without
 * materialising objects for the ones it discards.
 *
 * <h2>Slot layout</h2>
 * <pre>
 *   slot[n]   = nodeId(16) | keyId(16) | unused(24) | tag(8)
 *   slot[n+1] = the value's raw bits
 * </pre>
 * Every entry is exactly two slots whatever the value type, which is what lets a reader skip without
 * decoding.
 */
public final class BinaryRecordDecoder {

    /** Tags, mirroring {@link BinaryLogRecord}. */
    public static final int TAG_DOUBLE = 1, TAG_LONG = 2, TAG_INT = 3, TAG_CHAR = 4,
            TAG_CHARSEQ = 5, TAG_OBJECT = 6, TAG_BOOL = 7;
    /**
     * A node was invoked. The entry carries a node id and no key or value — {@code keyId} is 0 and the
     * value slot is 0 — but it is still exactly two slots, because "every entry is two slots" is the
     * property that lets a reader skip an entry without decoding it.
     */
    public static final int TAG_TRACE = 8;

    /** Receives each decoded entry. Primitives only — nothing is allocated to report an entry. */
    public interface Visitor {
        void onEntry(int nodeId, int keyId, int tag, long rawBits);
    }

    private BinaryRecordDecoder() {
    }

    public static int nodeId(long header) {
        return (int) (header >>> 48) & 0xFFFF;
    }

    public static int keyId(long header) {
        return (int) (header >>> 32) & 0xFFFF;
    }

    public static int tag(long header) {
        return (int) (header & 0xFF);
    }

    /** {@code true} if the tag is one this decoder understands. */
    public static boolean knownTag(int tag) {
        return tag >= TAG_DOUBLE && tag <= TAG_TRACE;
    }

    /**
     * Walks the entries in {@code slots[0 .. lengthBytes/8)}.
     *
     * @param lengthBytes {@link BinaryLogRecord#length()} — bytes, not slots
     * @return the number of entries visited
     * @throws IllegalArgumentException if the length is not a whole number of two-slot entries, which
     *                                  means the record is truncated and should be reported rather than
     *                                  silently half-read
     */
    public static int decode(long[] slots, int lengthBytes, Visitor visitor) {
        if (lengthBytes % 16 != 0) {
            throw new IllegalArgumentException("truncated record: " + lengthBytes
                    + " bytes is not a whole number of 16-byte entries");
        }
        int slotCount = lengthBytes / 8;
        if (slotCount > slots.length) {
            throw new IllegalArgumentException("record claims " + slotCount
                    + " slots but the buffer holds " + slots.length);
        }
        int entries = 0;
        for (int i = 0; i + 1 < slotCount; i += 2) {
            long header = slots[i];
            visitor.onEntry(nodeId(header), keyId(header), tag(header), slots[i + 1]);
            entries++;
        }
        return entries;
    }

    /** Renders a value the way the text record would, so decoded output is comparable to a YAML log. */
    public static String renderValue(int tag, long rawBits) {
        switch (tag) {
            case TAG_DOUBLE:  return Double.toString(Double.longBitsToDouble(rawBits));
            case TAG_LONG:    return Long.toString(rawBits);
            case TAG_INT:     return Integer.toString((int) rawBits);
            case TAG_CHAR:    return String.valueOf((char) rawBits);
            case TAG_BOOL:    return rawBits != 0 ? "true" : "false";
            case TAG_TRACE:   return "";
            default:          return "#tag" + tag + ":" + rawBits;
        }
    }
}
