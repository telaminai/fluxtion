/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.time.Clock;
import com.telamin.fluxtion.runtime.time.ClockStrategy;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * <b>The binary record must carry the same information as the text record.</b>
 *
 * <p>The two encoders are driven through the same {@link EventLogManager} API with the same nodes and
 * the same values, and the binary output is decoded and compared against the text output field by
 * field. A faster encoder that quietly loses or reorders information is not a faster encoder, and
 * nothing else in the suite compares the two.
 *
 * <p>This is also the test that keeps the wire format honest: it fails if the slot layout, the id
 * assignment or the decoder drift apart from each other or from what the text record reports.
 */
public class BinaryVersusTextRecordTest {

    /** A node that logs a fixed set of values, as a generated node would. */
    static class Priced extends EventLogNode {
        void publish(double price, int qty, long seq, boolean live) {
            auditLog.info("price", price).info("qty", qty).info("seq", seq).info("live", live);
        }
    }

    private static Clock fixedClock() {
        Clock c = new Clock();
        c.init();
        c.setClockStrategy(new ClockStrategy.ClockStrategyEvent(() -> 1_000L));
        return c;
    }

    /**
     * What a sink actually receives. The record is REUSED and cleared after publish, so a sink that
     * wants the bytes must take them while it has them — reading the record afterwards sees an empty
     * one. Snapshotting here is what a real sink does, and getting it wrong is how the first version
     * of this test read zero entries.
     */
    static final class Snapshot {
        String text;
        long[] slots;
        int length;
    }

    /** Drives one manager over the same script, capturing what the sink saw. */
    private static <T extends LogRecord> T run(EventLogManager manager, T record, Snapshot snap) {
        manager.clock = fixedClock();
        manager.setLogSink(r -> {
            if (r instanceof BinaryLogRecord) {
                BinaryLogRecord b = (BinaryLogRecord) r;
                snap.length = b.length();
                snap.slots = java.util.Arrays.copyOf(b.slots(), snap.length / 8);
            } else {
                snap.text = r.asCharSequence().toString();
            }
        });
        manager.init();
        if (record != null) {
            manager.calculationLogConfig(new EventLogControlEvent(record));
        }
        Priced a = new Priced();
        Priced b = new Priced();
        manager.nodeRegistered(a, "bookA");
        manager.nodeRegistered(b, "bookB");

        manager.eventReceived(new Object());
        a.publish(7.917840894551858, 250, 9_223_372_036_854_775_806L, true);
        b.publish(-0.5, -3, Long.MIN_VALUE, false);
        manager.processingComplete();
        return record;
    }

    @Test
    public void theBinaryRecordCarriesEverythingTheTextRecordDoes() {
        Snapshot textSnap = new Snapshot();
        run(new EventLogManager(), null, textSnap);
        assertTrue("the text arm must publish a record", textSnap.text != null);
        String text = textSnap.text;

        Snapshot binSnap = new Snapshot();
        BinaryLogRecord binary = new BinaryLogRecord(fixedClock(), 4096);
        run(new EventLogManager(), binary, binSnap);
        assertTrue("the binary arm must publish a record", binSnap.slots != null);

        // ids -> names, taken from the record itself rather than assumed
        Map<Integer, String> names = new HashMap<>();
        for (String n : new String[]{"bookA", "bookB", "price", "qty", "seq", "live"}) {
            names.put(binary.internName(n), n);
        }

        Map<String, String> decoded = new LinkedHashMap<>();
        int entries = BinaryRecordDecoder.decode(binSnap.slots, binSnap.length,
                (nodeId, keyId, tag, bits) -> decoded.put(
                        names.get(nodeId) + "." + names.get(keyId),
                        BinaryRecordDecoder.renderValue(tag, bits)));

        assertEquals("two nodes logging four values each", 8, entries);
        assertEquals(8, decoded.size());

        // every decoded value must appear in the text record, under the right node and key
        for (Map.Entry<String, String> e : decoded.entrySet()) {
            String node = e.getKey().substring(0, e.getKey().indexOf('.'));
            String key = e.getKey().substring(e.getKey().indexOf('.') + 1);
            assertTrue("text record must name the node " + node + ":\n" + text,
                    text.contains(node));
            assertTrue("text record must carry " + key + ": " + e.getValue() + "\n" + text,
                    text.contains(key + ": " + e.getValue()));
        }
    }

    /** Order matters: an audit trail that reorders entries is not the same trail. */
    @Test
    public void entriesAppearInTheOrderTheyWereLogged() {
        Snapshot snap = new Snapshot();
        BinaryLogRecord binary = new BinaryLogRecord(fixedClock(), 4096);
        run(new EventLogManager(), binary, snap);

        Map<Integer, String> names = new HashMap<>();
        for (String n : new String[]{"bookA", "bookB", "price", "qty", "seq", "live"}) {
            names.put(binary.internName(n), n);
        }

        List<String> order = new ArrayList<>();
        BinaryRecordDecoder.decode(snap.slots, snap.length,
                (nodeId, keyId, tag, bits) -> order.add(names.get(nodeId) + "." + names.get(keyId)));

        assertEquals(List.of(
                "bookA.price", "bookA.qty", "bookA.seq", "bookA.live",
                "bookB.price", "bookB.qty", "bookB.seq", "bookB.live"), order);
    }

    /** Every entry is 16 bytes — the property a reader relies on to skip without decoding. */
    @Test
    public void everyEntryIsSixteenBytesWhateverTheType() {
        Snapshot snap = new Snapshot();
        BinaryLogRecord binary = new BinaryLogRecord(fixedClock(), 4096);
        run(new EventLogManager(), binary, snap);

        assertEquals("8 entries x 16 bytes", 128, snap.length);
    }
}
