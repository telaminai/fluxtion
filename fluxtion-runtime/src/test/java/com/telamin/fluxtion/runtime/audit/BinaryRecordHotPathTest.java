package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;
import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * The per-event and per-entry hot path of {@link BinaryLogRecord}, after a profile of the audited graph
 * showed two costs that were structural rather than necessary.
 *
 * <p>Every assertion here is a behaviour a benchmark cannot see: the record still publishes, the ids are
 * still stable, the dictionary still resolves. Speed is measured elsewhere; this holds the meaning fixed
 * while that changes.
 */
public class BinaryRecordHotPathTest {

    /**
     * {@code sourceRef} is now resolved in the logger's constructor rather than on first use. The risk
     * that creates is a logger resolved against one record and then writing into another — ids are
     * per-record, so a stale one would mis-label every entry. {@code EventLogManager} builds fresh
     * loggers whenever the record changes; this holds that contract at the logger level.
     */
    @Test
    public void aLoggerResolvesItsNodeIdAgainstTheRecordItWillWriteTo() {
        BinaryLogRecord first = record();
        // burn ids in the first record so the two id spaces cannot coincide by luck
        for (int i = 0; i < 7; i++) {
            first.internName("filler" + i);
        }
        EventLogger onFirst = new BinaryEventLogger(first, "nodeA");
        onFirst.setLevel(LogLevel.INFO);
        onFirst.info("v", 1.0);
        long idInFirst = (first.slots()[0] >>> 48) & 0xFFFF;

        BinaryLogRecord second = record();
        EventLogger onSecond = new BinaryEventLogger(second, "nodeA");
        onSecond.setLevel(LogLevel.INFO);
        onSecond.info("v", 1.0);
        long idInSecond = (second.slots()[0] >>> 48) & 0xFFFF;

        assertEquals("each record names nodeA through its OWN dictionary",
                "nodeA", first.dictionary()[(int) idInFirst]);
        assertEquals("nodeA", second.dictionary()[(int) idInSecond]);
        assertNotEquals("the two id spaces really are different, so this test can fail",
                idInFirst, idInSecond);
    }

    /**
     * {@code canLog} reads a cached int rather than dereferencing the level enum on every entry. The
     * cache is written only by {@code setLevel}, so the property to hold is that it never disagrees with
     * the reference it mirrors — including before any level is set, where the reference form relied on a
     * null check.
     */
    @Test
    public void aLoggerWithNoLevelSetLogsNothing() {
        BinaryLogRecord r = record();
        EventLogger logger = new BinaryEventLogger(r, "nodeA");
        r.triggerObject(new E1());
        logger.info("v", 1.0);
        logger.info();
        assertEquals("no level was ever set, so nothing may be recorded", 0, r.length());
    }

    @Test
    public void everyLevelGateAgreesWithTheEnumItMirrors() {
        for (LogLevel set : LogLevel.values()) {
            for (LogLevel at : LogLevel.values()) {
                BinaryLogRecord r = record();
                EventLogger logger = new BinaryEventLogger(r, "nodeA");
                logger.setLevel(set);
                r.triggerObject(new E1());
                logger.log("v", 1.0, at);
                boolean expected = set.level >= at.level;
                assertEquals("level " + set + " logging at " + at,
                        expected ? 16 : 0, r.length());
            }
        }
    }

    static class E1 implements com.telamin.fluxtion.runtime.event.Event { }

    static class E2 implements com.telamin.fluxtion.runtime.event.Event { }

    private static BinaryLogRecord record() {
        Clock clock = new Clock();
        clock.init();
        BinaryLogRecord r = new BinaryLogRecord(clock, 4096);
        r.updateLogLevel(LogLevel.INFO);
        return r;
    }

    /**
     * {@code writeSlots} no longer clears {@code firstProp}; {@code terminateRecord} reads {@code slot}
     * instead. If that is wrong, records stop being published and every audited path goes silently
     * empty — which is exactly the failure a fast benchmark rewards.
     */
    @Test
    public void aRecordWithIdPathEntriesReportsItselfAsLogged() {
        BinaryLogRecord r = record();
        r.triggerObject(new E1());
        r.addRecord(r.internName("nodeA"), r.internName("v"), 1.0);
        assertTrue("an entry was written, so the record must publish", r.terminateRecord());
    }

    @Test
    public void aRecordWithNoEntriesReportsItselfAsNotLogged() {
        BinaryLogRecord r = record();
        r.triggerObject(new E1());
        assertFalse("nothing was written, so nothing should publish", r.terminateRecord());
    }

    @Test
    public void aRecordWithOnlyStringPathEntriesStillReportsItselfAsLogged() {
        BinaryLogRecord r = record();
        r.triggerObject(new E1());
        r.addRecord("nodeA", "v", 1.0);
        assertTrue("the String path sets firstProp and must still publish", r.terminateRecord());
    }

    @Test
    public void aRecordWithOnlyATraceEntryStillReportsItselfAsLogged() {
        BinaryLogRecord r = record();
        r.triggerObject(new E1());
        r.addTrace("nodeA");
        assertTrue("a trace entry is an entry", r.terminateRecord());
    }

    /**
     * A trace is a normal two-slot entry: node id, no key, no value, {@code TAG_TRACE}. The fixed entry
     * size is the property a reader depends on to skip an entry without decoding it, so a trace must not
     * be a special case in the layout even though it is one in meaning.
     */
    @Test
    public void aTraceIsATwoSlotEntryNamingTheNodeAndNothingElse() {
        BinaryLogRecord r = record();
        r.triggerObject(new E1());
        r.addTrace("nodeA");

        assertEquals("exactly one entry, two slots, sixteen bytes", 16, r.length());
        long header = r.slots()[0];
        assertEquals("the node is named", "nodeA",
                r.dictionary()[(int) ((header >>> 48) & 0xFFFF)]);
        assertEquals("a trace has no key", 0, (int) ((header >>> 32) & 0xFFFF));
        assertEquals("and carries the trace tag", BinaryRecordDecoder.TAG_TRACE,
                BinaryRecordDecoder.tag(header));
        assertEquals("and no value", 0L, r.slots()[1]);
    }

    @Test
    public void theDecoderRecognisesATraceEntry() {
        assertTrue("an unknown tag makes a reader guess; this one is known",
                BinaryRecordDecoder.knownTag(BinaryRecordDecoder.TAG_TRACE));
        assertEquals("a trace has no value to render", "",
                BinaryRecordDecoder.renderValue(BinaryRecordDecoder.TAG_TRACE, 0L));
    }

    /** Traces and values interleave in one record, in the order they were written. */
    @Test
    public void tracesAndValuesShareOneRecordInOrder() {
        BinaryLogRecord r = record();
        r.triggerObject(new E1());
        r.addTrace("nodeA");
        r.addRecord(r.internName("nodeA"), r.internName("v"), 1.5);
        r.addTrace("nodeB");

        assertEquals("three entries", 3 * 16, r.length());
        long[] slots = r.slots();
        assertEquals(BinaryRecordDecoder.TAG_TRACE, BinaryRecordDecoder.tag(slots[0]));
        assertEquals(BinaryRecordDecoder.TAG_DOUBLE, BinaryRecordDecoder.tag(slots[2]));
        assertEquals(1.5, Double.longBitsToDouble(slots[3]), 0.0);
        assertEquals(BinaryRecordDecoder.TAG_TRACE, BinaryRecordDecoder.tag(slots[4]));
        assertEquals("nodeB", r.dictionary()[(int) ((slots[4] >>> 48) & 0xFFFF)]);
    }

    /** The logger's own tracing entry point, which is how a traced processor actually reaches this. */
    @Test
    public void theLoggerTracesThroughToTheRecord() {
        BinaryLogRecord r = record();
        EventLogger logger = new BinaryEventLogger(r, "nodeA");
        logger.setLevel(LogLevel.INFO);
        r.triggerObject(new E1());
        logger.info();
        assertEquals("logger.info() with no key is a node-invocation trace", 16, r.length());
        assertEquals(BinaryRecordDecoder.TAG_TRACE, BinaryRecordDecoder.tag(r.slots()[0]));
        assertEquals("nodeA", r.dictionary()[(int) ((r.slots()[0] >>> 48) & 0xFFFF)]);
    }

    @Test
    public void aTraceBelowTheLogLevelIsNotWritten() {
        BinaryLogRecord r = record();
        EventLogger logger = new BinaryEventLogger(r, "nodeA");
        logger.setLevel(LogLevel.WARN);
        r.triggerObject(new E1());
        logger.info();
        assertEquals("an INFO trace under a WARN level records nothing", 0, r.length());
    }

    /** terminateRecord resets the flag, so a reused record must not claim the previous event's entries. */
    @Test
    public void anEmptyEventAfterALoggedOneDoesNotInheritItsVerdict() {
        BinaryLogRecord r = record();
        r.triggerObject(new E1());
        r.addRecord(r.internName("nodeA"), r.internName("v"), 1.0);
        assertTrue(r.terminateRecord());
        r.triggerObject(new E1());
        assertFalse("header() reset slot, so the second event has nothing of its own",
                r.terminateRecord());
    }

    /**
     * The event type id now resolves through the identity table rather than the fallback map. The table
     * probes 8 slots and then gives up to the map, so the property to hold is that the answer is the
     * same either way — a type must keep one id for the life of the record.
     */
    @Test
    public void anEventTypeKeepsOneIdAcrossEvents() {
        BinaryLogRecord r = record();
        r.triggerObject(new E1());
        int first = r.eventTypeId();
        for (int i = 0; i < 1000; i++) {
            r.triggerObject(new E1());
        }
        assertEquals("the id must not drift as the table is re-probed", first, r.eventTypeId());
    }

    @Test
    public void distinctEventTypesGetDistinctIds() {
        BinaryLogRecord r = record();
        r.triggerObject(new E1());
        int one = r.eventTypeId();
        r.triggerObject(new E2());
        int two = r.eventTypeId();
        assertNotEquals("two types sharing an id would mis-label every record", one, two);
        r.triggerObject(new E1());
        assertEquals("and the first type keeps its own", one, r.eventTypeId());
    }

    @Test
    public void theEventTypeIdResolvesBackThroughTheDictionary() {
        BinaryLogRecord r = record();
        r.triggerObject(new E1());
        String[] dict = r.dictionary();
        assertEquals("a reader must be able to name the event type",
                E1.class.getName(), dict[r.eventTypeId()]);
    }

    /**
     * The first two keys of a logger are fields; the third and beyond spill to the array. All of them
     * must resolve, and each to its own id.
     */
    @Test
    public void aLoggerResolvesKeysPastTheTwoInlineSlots() {
        BinaryLogRecord r = record();
        EventLogger logger = new BinaryEventLogger(r, "nodeA");
        logger.setLevel(LogLevel.INFO);
        String[] keys = {"a", "b", "c", "d", "e", "f"};
        for (String k : keys) {
            logger.info(k, 1.0);
        }
        assertEquals("six distinct keys, six entries", 6 * 16, r.length());
        long[] slots = r.slots();
        java.util.Set<Long> keyIds = new java.util.HashSet<>();
        for (int i = 0; i < 6; i++) {
            keyIds.add((slots[i * 2] >>> 32) & 0xFFFF);
        }
        assertEquals("every key must have its own id, inline slot or not", 6, keyIds.size());
    }

    @Test
    public void repeatingAKeyPastTheInlineSlotsReturnsTheSameId() {
        BinaryLogRecord r = record();
        EventLogger logger = new BinaryEventLogger(r, "nodeA");
        logger.setLevel(LogLevel.INFO);
        for (String k : new String[]{"a", "b", "c", "d"}) {
            logger.info(k, 1.0);
        }
        long fourth = (r.slots()[3 * 2] >>> 32) & 0xFFFF;
        r.clear();
        logger.info("d", 2.0);
        assertEquals("the spilled key must resolve to the id it was first given",
                fourth, (r.slots()[0] >>> 32) & 0xFFFF);
    }
}
