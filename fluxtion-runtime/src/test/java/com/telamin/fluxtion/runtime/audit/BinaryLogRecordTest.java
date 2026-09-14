/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link BinaryLogRecord} writes entries as two aligned {@code long} slots. These tests decode the slots
 * back and assert the values survive exactly — the encoder is only worth having if what comes out is
 * what went in.
 *
 * <p>The slot layout under test:
 * <pre>
 *   slot[n]   = nodeId(16) | keyId(16) | tag(8)      packed into a long
 *   slot[n+1] = the value's raw bits
 * </pre>
 */
public class BinaryLogRecordTest {

    private BinaryLogRecord record;

    @Before
    public void setUp() {
        Clock clock = new Clock();
        clock.init();
        record = new BinaryLogRecord(clock, 4096);
        record.updateLogLevel(EventLogControlEvent.LogLevel.INFO);
        record.triggerObject(new Object());
    }

    private static int nodeIdOf(long header) { return (int) (header >>> 48) & 0xFFFF; }

    private static int keyIdOf(long header) { return (int) (header >>> 32) & 0xFFFF; }

    private static int tagOf(long header) { return (int) (header & 0xFF); }

    @Test
    public void aDoubleSurvivesToTheLastBit() {
        int node = record.internName("nodeA");
        int key = record.internName("price");
        double value = 7.917840894551858;   // deliberately not representable in few digits

        record.addRecord(node, key, value);

        long[] slots = record.slots();
        assertEquals("one entry is two slots", 16, record.length());
        assertEquals(node, nodeIdOf(slots[0]));
        assertEquals(key, keyIdOf(slots[0]));
        assertEquals("raw bits, so exact — not a shortest-repr round trip",
                value, Double.longBitsToDouble(slots[1]), 0.0);
    }

    @Test
    public void longIntAndBooleanSurvive() {
        int node = record.internName("nodeA");
        record.addRecord(node, record.internName("count"), Long.MIN_VALUE);
        record.addRecord(node, record.internName("size"), Integer.MAX_VALUE);
        record.addRecord(node, record.internName("flag"), true);

        long[] slots = record.slots();
        assertEquals(48, record.length());
        assertEquals(Long.MIN_VALUE, slots[1]);
        assertEquals(Integer.MAX_VALUE, (int) slots[3]);
        assertEquals(1L, slots[5]);
    }

    /** Names resolve to stable ids, and the same name always yields the same id. */
    @Test
    public void nameIdsAreStableAndDistinct() {
        int a1 = record.internName("alpha");
        int b = record.internName("beta");
        int a2 = record.internName("alpha");

        assertEquals("the same name must resolve to the same id", a1, a2);
        assertTrue("distinct names must get distinct ids", a1 != b);
        assertTrue("ids are non-negative, NO_ID means 'this record does not use ids'",
                a1 >= 0 && b >= 0);
    }

    /** A record is reused across events; each event must start clean. */
    @Test
    public void triggerResetsTheRecordSoEventsDoNotBleedTogether() {
        int node = record.internName("nodeA");
        record.addRecord(node, record.internName("v"), 1.0);
        assertEquals(16, record.length());

        record.triggerObject(new Object());
        assertEquals("a new event starts an empty record", 0, record.length());

        record.addRecord(node, record.internName("v"), 2.0);
        assertEquals(16, record.length());
        assertEquals(2.0, Double.longBitsToDouble(record.slots()[1]), 0.0);
    }

    /** terminateRecord reports whether anything was logged — the sink is skipped when nothing was. */
    @Test
    public void terminateReportsWhetherAnythingWasLogged() {
        assertFalse("nothing logged yet", record.terminateRecord());

        record.triggerObject(new Object());
        record.addRecord(record.internName("nodeA"), record.internName("v"), 1.0);
        assertTrue("an entry was written", record.terminateRecord());
    }

    /** Overflow must be reported, not silently truncate or corrupt. */
    @Test
    public void overflowIsFlaggedRatherThanSilentlyLosingData() {
        BinaryLogRecord tiny = new BinaryLogRecord(new Clock(), 16);   // 4 slots
        tiny.updateLogLevel(EventLogControlEvent.LogLevel.INFO);
        int node = tiny.internName("n");
        int key = tiny.internName("k");

        for (int i = 0; i < 10; i++) {
            tiny.addRecord(node, key, (double) i);
        }

        assertTrue("running past the buffer must be visible to the caller", tiny.overflowed());
        assertTrue("and it must not write past the end", tiny.length() <= 16 * 8);
    }

    /** Every entry is exactly two slots, whatever the value type — the reader depends on it. */
    @Test
    public void everyEntryIsExactlyTwoSlots() {
        int node = record.internName("nodeA");
        record.addRecord(node, record.internName("a"), 1.0);
        record.addRecord(node, record.internName("b"), 2L);
        record.addRecord(node, record.internName("c"), 3);
        record.addRecord(node, record.internName("d"), false);

        assertEquals("4 entries x 2 slots x 8 bytes", 64, record.length());
    }
}
