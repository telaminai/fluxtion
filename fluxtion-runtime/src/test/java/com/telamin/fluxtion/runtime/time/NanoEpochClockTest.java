package com.telamin.fluxtion.runtime.time;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * The fast clock exists because the default one cannot represent the thing it is read for: at
 * millisecond resolution {@code endTime - logTime} is zero for every sub-millisecond event, so the
 * framework pays two {@code System.currentTimeMillis()} calls per event for a duration that is
 * structurally always 0.
 */
public class NanoEpochClockTest {

    @Test
    public void itResolvesFinerThanAMillisecond() {
        ClockStrategy clock = ClockStrategy.nanoEpochClock();
        long first = clock.getWallClockTime();
        long distinct = 0, last = first;
        for (int i = 0; i < 100_000; i++) {
            long v = clock.getWallClockTime();
            if (v != last) { distinct++; last = v; }
        }
        assertTrue("a clock that cannot separate two adjacent reads cannot measure a 40ns event; "
                + "distinct readings=" + distinct, distinct > 1_000);
    }

    @Test
    public void itIsMonotonic() {
        ClockStrategy clock = ClockStrategy.nanoEpochClock();
        long last = clock.getWallClockTime();
        for (int i = 0; i < 200_000; i++) {
            long v = clock.getWallClockTime();
            assertTrue("a clock that steps backwards makes a duration negative", v >= last);
            last = v;
        }
    }

    @Test
    public void itIsAnchoredNearWallClockTime() {
        ClockStrategy clock = ClockStrategy.nanoEpochClock();
        long wallMillis = System.currentTimeMillis();
        long clockMillis = clock.getWallClockTime() / 1_000_000L;
        assertTrue("nanos since the epoch must still name roughly now, was " + clockMillis
                + " against " + wallMillis, Math.abs(clockMillis - wallMillis) < 1_000);
    }

    @Test
    public void aDurationAcrossItIsNotAlwaysZero() {
        ClockStrategy clock = ClockStrategy.nanoEpochClock();
        long t0 = clock.getWallClockTime();
        long sink = 0;
        for (int i = 0; i < 10_000; i++) { sink += i; }
        long elapsed = clock.getWallClockTime() - t0;
        assertTrue("the whole point: real work must show a non-zero duration (sink=" + sink + ")",
                elapsed > 0);
    }
}
