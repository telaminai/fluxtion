package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;
import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/**
 * The ordinal-key write path: a node declares its keys once and logs by position, which is what a code
 * model emits when it can see the call sites.
 *
 * <p>The property that matters is that it is a <em>pure</em> optimisation — the bytes on the wire are
 * identical to the String path, so a reader cannot tell which one wrote them, and no consumer needs to
 * know a processor was specialised. Everything else here is about the two ways this has already gone
 * wrong: declaring on the wrong object, and writing through the wrong reference.
 */
public class OrdinalKeyPathTest {

    private static BinaryLogRecord record() {
        BinaryLogRecord r = new BinaryLogRecord(new Clock(), 4096);
        r.updateLogLevel(LogLevel.INFO);
        return r;
    }

    private static long[] entries(BinaryLogRecord r) {
        return Arrays.copyOf(r.slots(), r.length() / 8);
    }

    @Test
    public void theOrdinalPathWritesExactlyWhatTheStringPathWrites() {
        BinaryLogRecord viaStrings = record();
        EventLogger s = new BinaryEventLogger(viaStrings, "nodeA");
        s.setLevel(LogLevel.INFO);
        s.info("v", 1.5).info("w", 2.5).info("v", 3.5);

        BinaryLogRecord viaOrdinals = record();
        EventLogger o = new BinaryEventLogger(viaOrdinals, "nodeA");
        o.setLevel(LogLevel.INFO);
        o.declareKeys("v", "w");
        o.info(0, 1.5).info(1, 2.5).info(0, 3.5);

        assertArrayEquals("the ordinal path is an optimisation, not a second wire format",
                entries(viaStrings), entries(viaOrdinals));
    }

    @Test
    public void aDeclaredKeyIsInternedOnceHoweverOftenItIsLogged() {
        CountingRecord r = new CountingRecord();
        EventLogger logger = new EventLogger(r, "nodeA");
        logger.setLevel(LogLevel.INFO);
        logger.declareKeys("v");
        for (int i = 0; i < 500; i++) {
            logger.info(0, (double) i);
        }
        assertEquals("500 entries were written", 500, r.entries);
        assertEquals("the key was interned once, not once per event", 1, r.internedKeys);
    }

    /**
     * The first version of this declared keys from an {@code @Initialise} method, which runs before the
     * manager installs a logger — so {@code auditLog} was still {@link NullEventLogger#INSTANCE} and
     * every node in the JVM declared its keys onto one shared singleton. {@code setLogger} is the hook
     * the logger actually arrives through.
     */
    @Test
    public void declaringOnTheNullLoggerCannotCorruptTheSingleton() {
        EventLogger before = NullEventLogger.INSTANCE;
        NullEventLogger.INSTANCE.declareKeys("v", "w");
        assertSame("the shared null logger is not replaced", before, NullEventLogger.INSTANCE);
        NullEventLogger.INSTANCE.info(0, 1.0);   // must not throw, must not record
        NullEventLogger.INSTANCE.info(9, 1.0);   // out of range on a logger that ignores everything
    }

    /**
     * {@link BinaryEventLogger} must override the ordinal writes as well as the String ones. The base
     * class holds the record as a {@link LogRecord}, so its {@code addRecord} is a virtual call; under
     * closed-world AOT that costs about what the ordinal key saves, and the optimisation nets to zero.
     * This asserts the override exists rather than the timing, which no unit test can hold.
     */
    @Test
    public void theBinaryLoggerWritesOrdinalsThroughItsConcreteRecord() throws Exception {
        assertEquals("BinaryEventLogger must declare log(int,double,LogLevel) itself",
                BinaryEventLogger.class,
                BinaryEventLogger.class.getDeclaredMethod(
                        "log", int.class, double.class, LogLevel.class).getDeclaringClass());
        assertEquals("BinaryEventLogger must declare log(int,long,LogLevel) itself",
                BinaryEventLogger.class,
                BinaryEventLogger.class.getDeclaredMethod(
                        "log", int.class, long.class, LogLevel.class).getDeclaringClass());
    }

    @Test
    public void keysBelowTheLogLevelAreNotWritten() {
        BinaryLogRecord r = record();
        EventLogger logger = new BinaryEventLogger(r, "nodeA");
        logger.setLevel(LogLevel.WARN);
        logger.declareKeys("v");
        logger.info(0, 1.0);
        assertEquals("an INFO write under a WARN level records nothing", 0, r.length());
    }

    @Test
    public void redeclaringKeysReplacesTheDeclarationRatherThanAppending() {
        BinaryLogRecord r = record();
        EventLogger logger = new BinaryEventLogger(r, "nodeA");
        logger.setLevel(LogLevel.INFO);
        logger.declareKeys("v");
        logger.declareKeys("w", "x");
        logger.info(1, 4.0);
        BinaryLogRecord expect = record();
        EventLogger s = new BinaryEventLogger(expect, "nodeA");
        s.setLevel(LogLevel.INFO);
        s.info("x", 4.0);
        assertArrayEquals("ordinal 1 must mean the second key of the LATEST declaration",
                entries(expect), entries(r));
    }

    /** A record that returns NO_ID has no id space, so a declared key must fall back, not corrupt. */
    @Test
    public void aRecordWithoutIdsStillRecordsThroughTheStringPath() {
        StringOnlyRecord r = new StringOnlyRecord();
        EventLogger logger = new EventLogger(r, "nodeA");
        logger.setLevel(LogLevel.INFO);
        logger.declareKeys("v");
        logger.info(0, 1.0);
        assertEquals("the entry is not silently dropped", 1, r.entries);
    }

    static class CountingRecord extends LogRecord {
        int entries;
        int internedKeys;
        CountingRecord() { super(new Clock()); }
        @Override public int internName(String name) {
            if (!"nodeA".equals(name)) { internedKeys++; }
            return name.hashCode() & 0xFFFF;
        }
        @Override public void addRecord(int sourceRef, int keyRef, double value) { entries++; }
    }

    static class StringOnlyRecord extends LogRecord {
        int entries;
        StringOnlyRecord() { super(new Clock()); }
        @Override public void addRecord(String sourceId, String propertyKey, double value) { entries++; }
    }
}
