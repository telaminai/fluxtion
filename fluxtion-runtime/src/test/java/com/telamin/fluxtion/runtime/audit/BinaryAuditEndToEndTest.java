package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.audit.tools.AuditLogFilter;
import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;
import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * The binary audit path driven END TO END, through the surfaces a user actually touches.
 *
 * <p><b>Why these are new.</b> A review found five defects on this path while every existing test was
 * green, because each of those tests stopped at a component boundary: one asserted the manager built a
 * binary record and stopped before publishing, another proved a String reached storage using its own
 * dictionary renderer rather than the production one. Each defect lived in the gap between two tested
 * pieces. These drive logger → record → writer → reader → CLI filter instead.
 */
public class BinaryAuditEndToEndTest {

    private EventLogManager binaryManager(LogRecordListener sink) {
        EventLogManager manager = new EventLogManager(sink).tracingOff().binaryRecord(true);
        manager.init();
        return manager;
    }

    /** The record a node logs into, wired the way the manager wires one. */
    private BinaryLogRecord record() {
        Clock clock = new Clock();
        clock.init();
        BinaryLogRecord r = new BinaryLogRecord(clock, 4096);
        r.updateLogLevel(LogLevel.INFO);
        r.triggerObject(new Object());
        return r;
    }

    /** A BinaryEventLogger with its own level set - a logger with no level records nothing. */
    private EventLogger loggerOn(BinaryLogRecord r) {
        EventLogger logger = new BinaryEventLogger(r, "node");
        logger.setLevel(LogLevel.INFO);
        return logger;
    }

    /**
     * A char is the one primitive that had no id/slot path, so it went to a byte buffer the binary
     * record abandons: the record called itself publishable and the file declared zero entries.
     */
    @Test
    public void aCharSurvivesToTheFile() throws Exception {
        Path file = Files.createTempFile("audit-char", ".flxa");
        BinaryLogRecord r = record();
        EventLogger logger = loggerOn(r);
        logger.info("c", 'x');
        r.terminateRecord();

        try (BinaryLogWriter w = new BinaryLogWriter(Files.newOutputStream(file))) {
            w.processLogRecord(r);
        }
        List<String> values = new ArrayList<>();
        BinaryLogReader.Result result = BinaryLogReader.read(file, new BinaryLogReader.Visitor() {
            public boolean onRecord(int a, String b, long c, long d, long e, int f) { return true; }
            public void onEntry(int nodeId, String node, int keyId, String key, int tag, long bits) {
                values.add(BinaryRecordDecoder.renderValue(tag, bits));
            }
        });
        assertEquals("the char entry must reach the file", 1, result.entries);
        assertEquals("and decode as its character", "x", values.get(0));
    }

    /**
     * String values are stored as dictionary ids. The shipped CLI renderer had no case for that tag and
     * printed the raw pair, so a real String showed up as {@code #tag5:4} rather than its text.
     */
    @Test
    public void aStringRendersAsItsValueThroughTheProductionFilter() throws Exception {
        Path file = Files.createTempFile("audit-string", ".flxa");
        BinaryLogRecord r = record();
        EventLogger logger = loggerOn(r);
        logger.info("venue", "DEMO");
        r.terminateRecord();
        try (BinaryLogWriter w = new BinaryLogWriter(Files.newOutputStream(file))) {
            w.processLogRecord(r);
        }

        List<String> rendered = new ArrayList<>();
        AuditLogFilter filter = new AuditLogFilter(null, null, null, Long.MIN_VALUE, Long.MAX_VALUE,
                Long.MAX_VALUE, new AuditLogFilter.Sink() {
            public void record(String eventType, long eventTime, long logTime, long endTime) { }
            public void entry(String node, String key, String value) { rendered.add(value); }
        });
        BinaryLogReader.read(file, filter);

        assertEquals("the production CLI path must render the string, not its tag/id pair",
                "DEMO", rendered.get(0));
    }

    /** A truncated record must not be written as a complete one. */
    @Test
    public void anOverflowedRecordIsRefusedRatherThanWrittenShort() {
        BinaryLogRecord r = record();
        EventLogger logger = loggerOn(r);
        // LITERAL keys. Names are interned by identity - a computed key is a new instance every call
        // and would exhaust the dictionary instead of overflowing the record, which is a different
        // defect with its own refusal.
        for (int i = 0; i < 100_000; i++) {
            logger.info("k", i);
        }
        assumeOverflowed(r);
        r.terminateRecord();
        try (BinaryLogWriter w = new BinaryLogWriter(new ByteArrayOutputStream())) {
            w.processLogRecord(r);
            fail("a record that dropped entries must not be written as a complete one");
        } catch (IllegalStateException refused) {
            assertTrue("the refusal must say entries were dropped, got: " + refused.getMessage(),
                    refused.getMessage().contains("overflowed"));
        } catch (Exception e) {
            fail("expected refusal, got " + e);
        }
    }

    private void assumeOverflowed(BinaryLogRecord r) {
        org.junit.Assume.assumeTrue("this fixture must overflow to be meaningful", r.overflowed());
    }

    /** A dictionary name longer than the u16 length field corrupted the file rather than being refused. */
    @Test
    public void anOversizeDictionaryNameIsRefusedRatherThanCorruptingTheFile() {
        BinaryLogRecord r = record();
        EventLogger logger = loggerOn(r);
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 70_000; i++) {
            huge.append('a');
        }
        logger.info("big", huge.toString());
        r.terminateRecord();
        try (BinaryLogWriter w = new BinaryLogWriter(new ByteArrayOutputStream())) {
            w.processLogRecord(r);
            fail("a name too long for the u16 length field must be refused, not silently truncated");
        } catch (IllegalStateException refused) {
            assertTrue("the refusal must name the limit, got: " + refused.getMessage(),
                    refused.getMessage().contains("65535"));
        } catch (Exception e) {
            fail("expected refusal, got " + e);
        }
    }

    /**
     * No sink installed: the refusal comes from the DEFAULT SINK at publish, naming the fix.
     *
     * <p>It was briefly refused at {@code init()} instead, which broke every generated processor:
     * generated code calls {@code EventLogManager.init()} from the PROCESSOR'S CONSTRUCTOR, so the
     * guard fired before any caller could retrieve the auditor and install a sink — 22 errors in the
     * compiler suite. The documented sequence is construct, retrieve the auditor, install the writer,
     * then {@code processor.init()}. Refusing at publish keeps that window open.
     */
    @Test
    public void binaryWithTheDefaultSinkRefusesAtPublishNamingTheFix() {
        EventLogManager manager = new EventLogManager().tracingOff().binaryRecord(true);
        manager.clock = new Clock();
        manager.clock.init();
        manager.init();     // MUST NOT throw - this is the generated processor's constructor
        manager.nodeRegistered(new Object(), "node");
        manager.eventReceived(new Object());
        try {
            manager.publishLastRecord();
            fail("a binary record reaching the default text sink must be refused");
        } catch (IllegalStateException refused) {
            String m = refused.getMessage();
            assertTrue("must name the sink type to install, got: " + m, m.contains("BinaryLogWriter"));
            assertTrue("must offer the text alternative, got: " + m, m.contains("TEXT"));
        }
    }

    /**
     * The STRING-KEY primitive overloads must reach the file.
     *
     * <p>They wrote to the byte buffer this record does not publish, so every one of them produced a
     * record that called itself publishable and a file declaring zero entries. char was fixed on the
     * INDEXED path and this family was left behind.
     */
    @Test
    public void stringKeyPrimitivesReachTheFile() throws Exception {
        Path file = Files.createTempFile("audit-stringkey", ".flxa");
        BinaryLogRecord r = record();
        r.addRecord("node", "d", 1.25d);
        r.addRecord("node", "l", 7L);
        r.addRecord("node", "i", 3);
        r.addRecord("node", "c", 'z');
        r.addRecord("node", "b", true);
        assertTrue("the record must report itself publishable", r.terminateRecord());
        try (BinaryLogWriter w = new BinaryLogWriter(Files.newOutputStream(file))) {
            w.processLogRecord(r);
        }
        List<String> values = new ArrayList<>();
        BinaryLogReader.Result result = BinaryLogReader.read(file, new BinaryLogReader.Visitor() {
            public boolean onRecord(int a, String b, long c, long d, long e, int f) { return true; }
            public void onEntry(int nodeId, String node, int keyId, String key, int tag, long bits) {
                values.add(BinaryRecordDecoder.renderValue(tag, bits));
            }
        });
        assertEquals("all five string-key primitives must reach the file", 5, result.entries);
        assertEquals("[1.25, 7, 3, z, true]", values.toString());
    }

    /**
     * A record REPLACED at runtime keeps writing a correct file.
     *
     * <p>{@code EventLogControlEvent} can swap the {@code LogRecord} without swapping the sink, and
     * the runtime documents that as supported. The new record re-interns names, so the same graph can
     * allocate the same ids to different names — which silently relabelled every later entry, then
     * (briefly) was refused outright. The writer translates instead: names are the identity.
     */
    @Test
    public void aReplacementRecordStillWritesCorrectNames() throws Exception {
        Path file = Files.createTempFile("audit-swap", ".flxa");
        Clock clock = new Clock();
        clock.init();
        try (BinaryLogWriter w = new BinaryLogWriter(Files.newOutputStream(file))) {
            BinaryLogRecord first = new BinaryLogRecord(clock, 4096);
            first.updateLogLevel(LogLevel.INFO);
            first.triggerObject(new Object());
            first.addRecord("z", "k", 1);
            first.terminateRecord();
            w.processLogRecord(first);

            // A DIFFERENT record instance, interning a different name into the same id.
            BinaryLogRecord second = new BinaryLogRecord(clock, 4096);
            second.updateLogLevel(LogLevel.INFO);
            second.triggerObject(new Object());
            second.addRecord("a", "k", 2);
            second.terminateRecord();
            w.processLogRecord(second);
        }
        List<String> nodes = new ArrayList<>();
        BinaryLogReader.read(file, new BinaryLogReader.Visitor() {
            public boolean onRecord(int a, String b, long c, long d, long e, int f) { return true; }
            public void onEntry(int nodeId, String node, int keyId, String key, int tag, long bits) {
                nodes.add(node);
            }
        });
        assertEquals("each entry must carry the name its own record used", "[z, a]", nodes.toString());
    }

    /** The dictionary-exhaustion refusal, pinned. */
    @Test
    public void anExhaustedDictionaryIsRefusedNamingTheIdentityTrap() {
        BinaryLogRecord r = record();
        try {
            for (int i = 0; i < Short.MAX_VALUE + 10; i++) {
                r.addRecord("node", "k", new String("v" + i));   // a fresh instance every call
            }
            fail("exhausting the dictionary must be refused, not wrapped to a negative id");
        } catch (IllegalStateException refused) {
            assertTrue("must explain identity interning, got: " + refused.getMessage(),
                    refused.getMessage().contains("IDENTITY"));
        }
    }

    /** Two distinct event types, so an untranslated header would be visible. */
    public static final class EventA { }
    public static final class EventB { }

    /**
     * THE PUBLIC RECORD-SWAP PATH, with an event type that changes. The earlier swap test used
     * {@code new Object()} on both sides, so the event id was identical in both records and the
     * untranslated header could not be seen: a B event was written as A, and the integrity counter
     * said the file was clean.
     */
    @Test
    public void aRecordSwappedThroughTheControlEventKeepsItsEventType() throws Exception {
        Path file = Files.createTempFile("audit-swap-event", ".flxa");
        EventLogManager manager;
        try (BinaryLogWriter w = new BinaryLogWriter(Files.newOutputStream(file))) {
            manager = new EventLogManager(w).tracingOff().binaryRecord(true);
            manager.clock = new Clock();
            manager.clock.init();
            manager.init();
            // A real EventLogSource node: registration hands it its logger, as generated code does.
            // The manager re-hands a logger on every record swap (setLogger is called again with a
            // logger bound to the NEW record), so the node must always log through the current one.
            // A first version of this test held the first logger and so wrote its third entry into
            // the retired record - the swap looked like it lost an event when the test had.
            EventLogger[] current = new EventLogger[1];
            manager.nodeRegistered((EventLogSource) log -> current[0] = log, "source");

            fire(manager, current, new EventA(), 1);
            fire(manager, current, new EventB(), 2);
            // the PUBLIC swap: a fresh record re-interns, so B can land on A's old id
            manager.calculationLogConfig(new EventLogControlEvent(new BinaryLogRecord(manager.clock, 4096)));
            fire(manager, current, new EventB(), 3);
        }
        List<String> events = new ArrayList<>();
        BinaryLogReader.read(file, new BinaryLogReader.Visitor() {
            public boolean onRecord(int id, String type, long a, long b, long c, int n) {
                events.add(type.substring(type.lastIndexOf('$') + 1)); return true; }
            public void onEntry(int a, String b, int c, String d, int e, long f) { }
        });
        assertEquals("the third event was B and must be recorded as B", "[EventA, EventB, EventB]",
                events.toString());
    }

    private static void fire(EventLogManager m, EventLogger[] current, Object event, int value) {
        m.eventReceived(event);
        current[0].info("k", value);
        m.processingComplete();
    }

    /** The entry count is a u16; a record that exceeds it must be refused, not wrapped to 0. */
    @Test
    public void tooManyEntriesForTheCountFieldIsRefused() {
        Clock clock = new Clock();
        clock.init();
        BinaryLogRecord r = new BinaryLogRecord(clock, 0xFFFF * 16 + 64);
        r.updateLogLevel(LogLevel.INFO);
        r.triggerObject(new Object());
        EventLogger logger = loggerOn(r);
        for (int i = 0; i <= 0xFFFF; i++) {
            logger.info("k", i);
        }
        org.junit.Assume.assumeFalse("fixture must not overflow the slot buffer", r.overflowed());
        r.terminateRecord();
        try (BinaryLogWriter w = new BinaryLogWriter(new ByteArrayOutputStream())) {
            w.processLogRecord(r);
            fail("65,536 entries must be refused - the count field wraps to 0");
        } catch (IllegalStateException refused) {
            assertTrue(refused.getMessage(), refused.getMessage().contains("65535"));
        } catch (Exception e) {
            fail("expected refusal, got " + e);
        }
    }

    /** The header carries the time unit, and a reader can see it. */
    @Test
    public void theHeaderCarriesTheTimeUnit() throws Exception {
        Path millis = Files.createTempFile("audit-unit-ms", ".flxa");
        Path nanos = Files.createTempFile("audit-unit-ns", ".flxa");
        for (Object[] c : new Object[][]{{millis, BinaryLogFile.TIME_UNIT_EPOCH_MILLIS},
                                         {nanos, BinaryLogFile.TIME_UNIT_EPOCH_NANOS}}) {
            BinaryLogRecord r = record();
            r.addRecord("n", "k", 1);
            r.terminateRecord();
            try (BinaryLogWriter w = new BinaryLogWriter(Files.newOutputStream((Path) c[0]), (int) c[1])) {
                w.processLogRecord(r);
            }
        }
        BinaryLogReader.Visitor ignore = new BinaryLogReader.Visitor() {
            public boolean onRecord(int a, String b, long c, long d, long e, int f) { return true; }
            public void onEntry(int a, String b, int c, String d, int e, long f) { }
        };
        assertEquals(BinaryLogFile.TIME_UNIT_EPOCH_MILLIS, BinaryLogReader.read(millis, ignore).timeUnit);
        assertEquals(BinaryLogFile.TIME_UNIT_EPOCH_NANOS, BinaryLogReader.read(nanos, ignore).timeUnit);
        // The DEFAULT constructor, not the explicit one re-read: the claim is about what a user who
        // states nothing gets, and the file above was written with the unit spelled out.
        Path byDefault = Files.createTempFile("audit-unit-default", ".flxa");
        BinaryLogRecord r = record();
        r.addRecord("n", "k", 1);
        r.terminateRecord();
        try (BinaryLogWriter w = new BinaryLogWriter(Files.newOutputStream(byDefault))) {
            w.processLogRecord(r);
        }
        assertEquals("the default writer declares milliseconds",
                BinaryLogFile.TIME_UNIT_EPOCH_MILLIS, BinaryLogReader.read(byDefault, ignore).timeUnit);
        assertEquals("and it is the header byte, not a reader default",
                BinaryLogFile.TIME_UNIT_EPOCH_MILLIS, Files.readAllBytes(byDefault)[7]);
    }

    /**
     * A record has TWO emission paths - the writer and its own {@code encodeTo} - and a review found
     * the second ran none of the first's checks: an overflowed record wrote a short frame, and 65,536
     * entries wrote a count of 0. Now one rule, {@code checkEncodable}, guards both, before any byte.
     */
    @Test
    public void everyEmissionPathRunsTheSameRepresentabilityCheck() throws Exception {
        // 1. Overflowed: refused by both, and neither wrote a byte.
        BinaryLogRecord overflowed = record();
        EventLogger overflowLogger = loggerOn(overflowed);
        for (int i = 0; i < 600; i++) {                       // a 4096-byte record holds 512 slots
            overflowLogger.info("k", i);
        }
        overflowed.terminateRecord();
        assertTrue("fixture must overflow", overflowed.overflowed());
        assertBothPathsRefuse(overflowed, "overflowed");

        // 2. Exactly the count field's capacity: written by both, count reads back as 65,535.
        BinaryLogRecord atCapacity = hugeRecord(0xFFFF);
        ByteArrayOutputStream viaWriter = new ByteArrayOutputStream();
        try (BinaryLogWriter w = new BinaryLogWriter(viaWriter)) {
            w.processLogRecord(atCapacity);
        }
        assertEquals(0xFFFF, countOfFirstRecord(viaWriter.toByteArray()));
        ByteArrayOutputStream viaEncodeTo = new ByteArrayOutputStream();
        atCapacity.encodeTo(viaEncodeTo);
        byte[] frame = viaEncodeTo.toByteArray();
        assertEquals(BinaryLogFile.FRAME_RECORD, frame[0]);
        assertEquals(0xFFFF, ((frame[1] & 0xFF) << 8) | (frame[2] & 0xFF));
        assertEquals("every slot byte follows the count",
                BinaryLogFile.RECORD_FIXED_BYTES + 0xFFFF * 16, frame.length);

        // 3. One more: refused by both. Before the fix encodeTo wrote count 0 and a megabyte of slots.
        assertBothPathsRefuse(hugeRecord(0xFFFF + 1), "65535");
    }

    private static void assertBothPathsRefuse(BinaryLogRecord r, String expectedInMessage) throws Exception {
        ByteArrayOutputStream viaWriter = new ByteArrayOutputStream();
        BinaryLogWriter w = new BinaryLogWriter(viaWriter);
        int headerOnly = viaWriter.size();
        try {
            w.processLogRecord(r);
            fail("writer must refuse");
        } catch (IllegalStateException refused) {
            assertTrue(refused.getMessage(), refused.getMessage().contains(expectedInMessage));
        }
        assertEquals("a refused record leaves the file as it was - no dictionary, no frame",
                headerOnly, viaWriter.size());
        ByteArrayOutputStream viaEncodeTo = new ByteArrayOutputStream();
        try {
            r.encodeTo(viaEncodeTo);
            fail("encodeTo must refuse with the same rule");
        } catch (IllegalStateException refused) {
            assertTrue(refused.getMessage(), refused.getMessage().contains(expectedInMessage));
        }
        assertEquals("nothing written by a refused encodeTo", 0, viaEncodeTo.size());
    }

    /** A record holding exactly {@code entries} entries, none dropped. */
    private BinaryLogRecord hugeRecord(int entries) {
        Clock clock = new Clock();
        clock.init();
        BinaryLogRecord r = new BinaryLogRecord(clock, entries * 16 + 64);
        r.updateLogLevel(LogLevel.INFO);
        r.triggerObject(new Object());
        EventLogger logger = loggerOn(r);
        for (int i = 0; i < entries; i++) {
            logger.info("k", i);
        }
        org.junit.Assume.assumeFalse("fixture must not overflow the slot buffer", r.overflowed());
        r.terminateRecord();
        return r;
    }

    /** Skips the header and any dictionary frames; returns the first RECORD frame's count field. */
    private static int countOfFirstRecord(byte[] file) {
        int p = BinaryLogFile.HEADER_BYTES;
        while (file[p] == BinaryLogFile.FRAME_DICT) {
            int len = ((file[p + 3] & 0xFF) << 8) | (file[p + 4] & 0xFF);
            p += 5 + len;
        }
        assertEquals(BinaryLogFile.FRAME_RECORD, file[p]);
        return ((file[p + 1] & 0xFF) << 8) | (file[p + 2] & 0xFF);
    }

    /**
     * The unit field is a u16. 65,537 wrapped to 1 and the file claimed milliseconds; 3 reached the
     * reader as 3. Now an undefined code is refused before the header, so a bad argument leaves no
     * file that looks valid.
     */
    @Test
    public void anUndefinedTimeUnitIsRefusedBeforeAnyHeaderByte() {
        // 65,536 would wrap to 0 ("unspecified") and 65,537 to 1 ("milliseconds") on the wire.
        for (int code : new int[]{3, 0x10000, 0x10001, 0x10002, -1}) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                new BinaryLogWriter(out, code);
                fail("code " + code + " must be refused");
            } catch (IllegalArgumentException refused) {
                assertTrue(refused.getMessage(), refused.getMessage().contains(String.valueOf(code)));
            }
            assertEquals("no header byte for code " + code, 0, out.size());
        }
        for (int code : new int[]{BinaryLogFile.TIME_UNIT_UNSPECIFIED, BinaryLogFile.TIME_UNIT_EPOCH_MILLIS,
                                  BinaryLogFile.TIME_UNIT_EPOCH_NANOS}) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new BinaryLogWriter(out, code);
            assertEquals("defined code " + code + " writes the header", BinaryLogFile.HEADER_BYTES, out.size());
        }
    }

    /**
     * A reader that presents timestamps in a fixed unit has to decide BEFORE records arrive. The
     * result's {@code timeUnit} is only known after {@code read} returns, by which time every record
     * was delivered; the analyser did exactly that and rejected a nanosecond file after consuming it.
     */
    @Test
    public void theHeaderReachesTheVisitorBeforeAnyRecord() throws Exception {
        Path nanos = Files.createTempFile("audit-header-first", ".flxa");
        BinaryLogRecord r = record();
        r.addRecord("n", "k", 1);
        r.terminateRecord();
        try (BinaryLogWriter w = new BinaryLogWriter(Files.newOutputStream(nanos), BinaryLogFile.TIME_UNIT_EPOCH_NANOS)) {
            w.processLogRecord(r);
            w.processLogRecord(r);
        }
        List<String> order = new ArrayList<>();
        BinaryLogReader.Visitor recording = new BinaryLogReader.Visitor() {
            public void onHeader(int version, int unit) { order.add("header:" + version + "/" + unit); }
            public void onDictionaryEntry(int id, String name) { order.add("dict"); }
            public boolean onRecord(int a, String b, long c, long d, long e, int f) { order.add("record"); return true; }
            public void onEntry(int a, String b, int c, String d, int e, long f) { order.add("entry"); }
        };
        BinaryLogReader.read(nanos, recording);
        assertEquals("header:" + BinaryLogFile.FORMAT_VERSION + "/" + BinaryLogFile.TIME_UNIT_EPOCH_NANOS, order.get(0));
        assertEquals(2, order.stream().filter("record"::equals).count());

        // A visitor that refuses at the header sees nothing else - on both read paths.
        class Refuses extends RuntimeException { }
        int[] delivered = new int[1];
        BinaryLogReader.Visitor refusing = new BinaryLogReader.Visitor() {
            public void onHeader(int version, int unit) {
                if (unit == BinaryLogFile.TIME_UNIT_EPOCH_NANOS) throw new Refuses();
            }
            public boolean onRecord(int a, String b, long c, long d, long e, int f) { delivered[0]++; return true; }
            public void onEntry(int a, String b, int c, String d, int e, long f) { delivered[0]++; }
            public void onDictionaryEntry(int id, String name) { delivered[0]++; }
        };
        try {
            BinaryLogReader.read(nanos, refusing);
            fail("must propagate the refusal");
        } catch (Refuses expected) { /* the point */ }
        try (java.io.InputStream in = Files.newInputStream(nanos)) {
            BinaryLogReader.readStreamed(in, 16, refusing);
            fail("must propagate the refusal on the streamed path too");
        } catch (Refuses expected) { /* the point */ }
        assertEquals("a refusal at the header delivers zero dictionary entries, records or entries",
                0, delivered[0]);
    }

    /**
     * The header unit describes the CLOCK STRATEGY's readings. An {@link com.telamin.fluxtion.runtime.event.Event}
     * supplies its own {@code eventTime} - by contract epoch milliseconds at construction - and the
     * runtime records it as given. This pins the documented split rather than hiding it: under a
     * nanosecond strategy a file carries nanosecond logTime/endTime and millisecond eventTime for
     * Event-typed events, and strategy-unit eventTime for anything else.
     */
    @Test
    public void anEventKeepsItsOwnMillisecondEventTimeUnderANanosecondStrategy() throws Exception {
        Clock clock = new Clock();
        clock.init();
        clock.setClockStrategy(new com.telamin.fluxtion.runtime.time.ClockStrategy.ClockStrategyEvent(
                com.telamin.fluxtion.runtime.time.ClockStrategy.nanoEpochClock()));
        BinaryLogRecord r = new BinaryLogRecord(clock, 4096);
        r.updateLogLevel(LogLevel.INFO);

        com.telamin.fluxtion.runtime.event.DefaultEvent typed = new com.telamin.fluxtion.runtime.event.DefaultEvent() { };
        clock.eventReceived(typed);              // what the generated processor does before the record starts
        r.triggerObject(typed);
        r.terminateRecord();
        Object plain = new Object();
        clock.eventReceived(plain);
        BinaryLogRecord r2 = new BinaryLogRecord(clock, 4096);
        r2.updateLogLevel(LogLevel.INFO);
        r2.triggerObject(plain);
        r2.terminateRecord();

        Path file = Files.createTempFile("audit-event-unit", ".flxa");
        try (BinaryLogWriter w = new BinaryLogWriter(Files.newOutputStream(file), BinaryLogFile.TIME_UNIT_EPOCH_NANOS)) {
            w.processLogRecord(r);
            w.processLogRecord(r2);
        }
        long millisFloor = 1_000_000_000_000L;        // 2001 in millis
        long nanosFloor = 1_000_000_000_000_000_000L; // 2001 in nanos
        List<long[]> times = new ArrayList<>();
        BinaryLogReader.read(file, new BinaryLogReader.Visitor() {
            public boolean onRecord(int a, String b, long ev, long log, long end, int f) { times.add(new long[]{ev, log, end}); return true; }
            public void onEntry(int a, String b, int c, String d, int e, long f) { }
        });
        long[] typedTimes = times.get(0);
        assertTrue("logTime is the strategy's reading, nanoseconds: " + typedTimes[1], typedTimes[1] > nanosFloor);
        assertTrue("endTime is the strategy's reading, nanoseconds: " + typedTimes[2], typedTimes[2] > nanosFloor);
        assertTrue("an Event's eventTime is the producer's milliseconds: " + typedTimes[0],
                typedTimes[0] > millisFloor && typedTimes[0] < 100_000_000_000_000L);
        long[] plainTimes = times.get(1);
        assertTrue("a plain object's eventTime is the strategy's reading: " + plainTimes[0], plainTimes[0] > nanosFloor);
    }
}
