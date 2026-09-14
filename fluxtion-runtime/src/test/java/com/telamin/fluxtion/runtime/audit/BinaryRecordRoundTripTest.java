/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Encoder and decoder must agree. A binary audit log that nothing can read back is not a feature, so
 * this is the test that makes the format a format rather than an internal detail.
 */
public class BinaryRecordRoundTripTest {

    private static BinaryLogRecord newRecord() {
        Clock clock = new Clock();
        clock.init();
        BinaryLogRecord r = new BinaryLogRecord(clock, 4096);
        r.updateLogLevel(EventLogControlEvent.LogLevel.INFO);
        r.triggerObject(new Object());
        return r;
    }

    private static List<String> decodeAll(BinaryLogRecord r, java.util.Map<Integer, String> names) {
        List<String> out = new ArrayList<>();
        int n = BinaryRecordDecoder.decode(r.slots(), r.length(),
                (nodeId, keyId, tag, bits) -> out.add(
                        names.get(nodeId) + "." + names.get(keyId) + "="
                                + BinaryRecordDecoder.renderValue(tag, bits)));
        assertEquals("visitor calls must match the reported entry count", n, out.size());
        return out;
    }

    @Test
    public void everyPrimitiveTypeRoundTrips() {
        BinaryLogRecord r = newRecord();
        int node = r.internName("orderBook");
        int px = r.internName("price");
        int qty = r.internName("qty");
        int seq = r.internName("seq");
        int live = r.internName("live");

        r.addRecord(node, px, 7.917840894551858);
        r.addRecord(node, qty, 250);
        r.addRecord(node, seq, 9_223_372_036_854_775_806L);
        r.addRecord(node, live, true);

        // ids are assigned by the record, and triggerObject interns the event class name first —
        // so the map is built from the ids actually returned rather than from a guessed order
        java.util.Map<Integer, String> names = new java.util.HashMap<>();
        names.put(node, "orderBook");
        names.put(px, "price");
        names.put(qty, "qty");
        names.put(seq, "seq");
        names.put(live, "live");
        List<String> decoded = decodeAll(r, names);

        assertEquals(4, decoded.size());
        assertEquals("orderBook.price=7.917840894551858", decoded.get(0));
        assertEquals("orderBook.qty=250", decoded.get(1));
        assertEquals("orderBook.seq=9223372036854775806", decoded.get(2));
        assertEquals("orderBook.live=true", decoded.get(3));
    }

    /** The reason for raw bits: a double survives exactly, not to a shortest representation. */
    @Test
    public void doublesSurviveBitExact() {
        BinaryLogRecord r = newRecord();
        int node = r.internName("n");
        int key = r.internName("k");
        double[] awkward = {
                Double.MIN_VALUE, Double.MAX_VALUE, -0.0, 1.0 / 3.0,
                Math.nextAfter(1.0, 2.0), 1e-300, -4.9e-324};

        for (double d : awkward) { r.addRecord(node, key, d); }

        List<Double> back = new ArrayList<>();
        BinaryRecordDecoder.decode(r.slots(), r.length(),
                (nodeId, keyId, tag, bits) -> back.add(Double.longBitsToDouble(bits)));

        assertEquals(awkward.length, back.size());
        for (int i = 0; i < awkward.length; i++) {
            assertEquals("bit-exact required, index " + i,
                    Double.doubleToRawLongBits(awkward[i]),
                    Double.doubleToRawLongBits(back.get(i)));
        }
    }

    /** Node and key ids must come back as written, including at the 16-bit boundary. */
    @Test
    public void nodeAndKeyIdsSurviveIncludingHighValues() {
        BinaryLogRecord r = newRecord();
        r.addRecord(0xFFFF, 0xFFFE, 1.0);
        r.addRecord(1, 2, 2.0);

        List<int[]> ids = new ArrayList<>();
        BinaryRecordDecoder.decode(r.slots(), r.length(),
                (nodeId, keyId, tag, bits) -> ids.add(new int[]{nodeId, keyId}));

        assertEquals(0xFFFF, ids.get(0)[0]);
        assertEquals(0xFFFE, ids.get(0)[1]);
        assertEquals(1, ids.get(1)[0]);
        assertEquals(2, ids.get(1)[1]);
    }

    /** A truncated record is reported, not half-read — the common case is a crashed process. */
    @Test
    public void aTruncatedRecordIsReportedRatherThanSilentlyHalfRead() {
        BinaryLogRecord r = newRecord();
        r.addRecord(r.internName("n"), r.internName("k"), 1.0);

        try {
            BinaryRecordDecoder.decode(r.slots(), r.length() - 8, (a, b, c, d) -> { });
            fail("half an entry must not decode silently");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("truncated"));
        }
    }

    /** A length beyond the buffer is a corrupt record, not an array index crash. */
    @Test
    public void aLengthBeyondTheBufferIsReportedClearly() {
        BinaryLogRecord r = newRecord();
        r.addRecord(r.internName("n"), r.internName("k"), 1.0);

        try {
            BinaryRecordDecoder.decode(r.slots(), (r.slots().length + 2) * 8, (a, b, c, d) -> { });
            fail("a length past the buffer must be reported");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("buffer holds"));
        }
    }

    /** An empty record decodes to nothing rather than failing. */
    @Test
    public void anEmptyRecordDecodesToZeroEntries() {
        BinaryLogRecord r = newRecord();
        assertEquals(0, BinaryRecordDecoder.decode(r.slots(), r.length(), (a, b, c, d) -> {
            throw new AssertionError("no entries expected");
        }));
    }
}
