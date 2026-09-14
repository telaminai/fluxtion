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

/**
 * {@link EventLogger} resolves node and key names to ids <b>once</b>, for records that want ids.
 *
 * <p>Every name reaching {@code addRecord} is a compile-time constant, and a record that encodes names
 * as integers otherwise resolves the same handful of strings on every event forever — measured at about
 * a third of a binary audit record. {@link EventLogger} is created per node and holds its node name in
 * a final field, so it is the right place to resolve.
 *
 * <p><b>Node code does not change.</b> These tests call {@code auditLog.info(key, value)} exactly as a
 * node would; only the record differs.
 */
public class EventLoggerIdPathTest {

    /** Counts how often it is asked to resolve a name — the whole point is "not per event". */
    static class IdRecord extends LogRecord {
        final List<String> resolved = new ArrayList<>();
        final List<String> entries = new ArrayList<>();
        private final List<String> names = new ArrayList<>();

        IdRecord(Clock clock) {
            super(clock);
            updateLogLevel(EventLogControlEvent.LogLevel.INFO);
        }

        @Override
        public int internName(String name) {
            resolved.add(name);
            int i = names.indexOf(name);
            if (i < 0) { names.add(name); i = names.size() - 1; }
            return i;
        }

        @Override
        public void addRecord(int sourceRef, int keyRef, double value) {
            entries.add(names.get(sourceRef) + "." + names.get(keyRef) + "=" + value);
        }
    }

    /** A record that declines ids — the existing behaviour, which must keep working untouched. */
    static class StringRecord extends LogRecord {
        final List<String> entries = new ArrayList<>();

        StringRecord(Clock clock) {
            super(clock);
            updateLogLevel(EventLogControlEvent.LogLevel.INFO);
        }

        @Override
        public void addRecord(String sourceId, String propertyKey, double value) {
            entries.add(sourceId + "." + propertyKey + "=" + value);
        }
    }

    private static Clock clock() {
        Clock c = new Clock();
        c.init();
        return c;
    }

    @Test
    public void namesAreResolvedOncePerLoggerNotOncePerEvent() {
        IdRecord record = new IdRecord(clock());
        EventLogger logger = new EventLogger(record, "nodeA");
        logger.setLevel(EventLogControlEvent.LogLevel.INFO);

        for (int i = 0; i < 100; i++) {
            logger.info("v", (double) i);
        }

        assertEquals("100 events must produce 100 entries", 100, record.entries.size());
        assertEquals("the node name and the key are each resolved exactly once, not 100 times",
                2, record.resolved.size());
        assertTrue(record.resolved.contains("nodeA"));
        assertTrue(record.resolved.contains("v"));
    }

    @Test
    public void theIdPathRecordsTheSameInformationAsTheStringPath() {
        IdRecord ids = new IdRecord(clock());
        StringRecord strings = new StringRecord(clock());
        EventLogger idLogger = new EventLogger(ids, "nodeA");
        EventLogger stringLogger = new EventLogger(strings, "nodeA");
        idLogger.setLevel(EventLogControlEvent.LogLevel.INFO);
        stringLogger.setLevel(EventLogControlEvent.LogLevel.INFO);

        idLogger.info("v", 1.5);
        stringLogger.info("v", 1.5);

        assertEquals(strings.entries, ids.entries);
    }

    /** More distinct keys than the logger's cache has slots — correctness must not depend on the cache. */
    @Test
    public void moreKeysThanCacheSlotsStillResolveCorrectly() {
        IdRecord record = new IdRecord(clock());
        EventLogger logger = new EventLogger(record, "nodeA");
        logger.setLevel(EventLogControlEvent.LogLevel.INFO);

        String[] keys = {"a", "b", "c", "d", "e", "f"};
        for (String k : keys) { logger.info(k, 1.0); }
        for (String k : keys) { logger.info(k, 2.0); }

        assertEquals(12, record.entries.size());
        for (int i = 0; i < keys.length; i++) {
            assertEquals("nodeA." + keys[i] + "=1.0", record.entries.get(i));
            assertEquals("nodeA." + keys[i] + "=2.0", record.entries.get(i + keys.length));
        }
    }

    /** A record that returns NO_ID keeps the String path — no existing subclass is disturbed. */
    @Test
    public void aRecordThatDeclinesIdsKeepsTheStringPath() {
        StringRecord record = new StringRecord(clock());
        EventLogger logger = new EventLogger(record, "nodeA");
        logger.setLevel(EventLogControlEvent.LogLevel.INFO);

        logger.info("v", 3.0);

        assertEquals(1, record.entries.size());
        assertEquals("nodeA.v=3.0", record.entries.get(0));
    }
}
