/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;

/**
 * A logger for an individual {@link EventLogSource} node. Users write values with
 * keys using one of the convenience methods. The {@link EventLogManager} will aggregate
 * all data into a {@link LogRecord} and publish to {@link LogRecordListener}.
 * <br>
 * <p>
 * The generated {@code LogRecord} is a structure that can be read by machines
 * and humans.
 *
 * @author Greg Higgins (greg.higgins@v12technology.com)
 */
public class EventLogger {

    private final LogRecord logrecord;
    private final String logSourceId;
    private LogLevel logLevel;
    /**
     * {@code logLevel.level}, kept alongside it. {@link #canLog} runs on every entry and read it through
     * the enum reference, which is a dependent load into a second object to compare two ints. The enum
     * is shared and cache-hot, so this is small — but it is on the per-entry path, and the field it
     * mirrors is written only by {@link #setLevel}.
     *
     * <p>{@link Integer#MIN_VALUE} when no level is set, so {@code canLog} is false without the null
     * check the reference form needed.
     */
    private int logLevelValue = Integer.MIN_VALUE;

    /**
     * Ids resolved once against {@link #logrecord}, if it uses them. {@code logSourceId} is final, so
     * the node's id never changes; keys are a tiny reference-compared cache because a node logs a small
     * fixed set of them. No hashing, no map — the measurement that motivated this showed the cost was
     * performing a lookup at all, so the fast path must be a reference compare and nothing more.
     */
    protected int sourceRef = LogRecord.NO_ID;
    /**
     * Resolved in the constructor, not on first use. Both the record and the node name arrive there, so
     * there was never anything to wait for — and the lazy form cost an {@code idsResolved} load and
     * branch on <b>every</b> entry to re-decide something decided once. A profile of the audited graph
     * put {@code useIds} at 13% of samples with nothing else left above it.
     *
     * <p>Not final only because {@link NullEventLogger} constructs without a record.
     */
    private boolean idsUsable;
    /**
     * The first two keys are held as <b>fields</b>, not array slots. Every node has its own logger, so
     * on a graph where 11.75 entries are written per event the array form touched three cache lines per
     * entry — the logger, its {@code keyNames} array and its {@code keyRefs} array — to answer a
     * question whose answer never changes. As fields they are on the logger object that had to be
     * loaded anyway.
     *
     * <p>A profile of the audited graph put {@code keyRef} at 37% of samples with the array form, which
     * is not the shape of "one reference compare": it is the shape of chasing pointers.
     *
     * <p>Two, because a node logging more than two distinct keys is rare and the overflow array below
     * keeps it correct rather than fast.
     */
    private String key0, key1;
    private int key0Ref, key1Ref;
    private static final int KEY_SLOTS = 4;
    private String[] keyNames;
    private int[] keyRefs;
    private int keyCount;

    /**
     * Whether this logger's record accepts integer ids. Exposed to subclasses so a record-specialised
     * logger can take the id path without duplicating the resolution.
     */
    protected boolean useIds() {
        return idsUsable;
    }

    /** The id for a property key, resolved once and cached by reference. Subclass-visible for the same
     *  reason as {@link #useIds()}. */
    protected int keyRef(String key) {
        // Identity, not equals: keys are string literals in generated node source, so they are interned
        // constants and the same reference arrives every call.
        if (key == key0) {
            return key0Ref;
        }
        if (key == key1) {
            return key1Ref;
        }
        return keyRefSlow(key);
    }

    /**
     * Everything past the second distinct key. Kept out of {@link #keyRef} so the hot path is two
     * reference compares and a return, with nothing for a compiler to decide not to inline.
     */
    private int keyRefSlow(String key) {
        if (keyNames != null) {
            for (int i = 0; i < keyCount; i++) {
                if (keyNames[i] == key) {
                    return keyRefs[i];
                }
            }
        }
        int ref = logrecord.internName(key);
        if (key0 == null) {
            key0 = key;
            key0Ref = ref;
        } else if (key1 == null) {
            key1 = key;
            key1Ref = ref;
        } else {
            if (keyNames == null) {
                keyNames = new String[KEY_SLOTS];
                keyRefs = new int[KEY_SLOTS];
            }
            if (keyCount < KEY_SLOTS) {
                keyNames[keyCount] = key;
                keyRefs[keyCount] = ref;
                keyCount++;
            }
        }
        return ref;
    }

    public EventLogger(LogRecord logrecord, String logSourceId) {
        this.logrecord = logrecord;
        this.logSourceId = logSourceId;
        logLevel = LogLevel.INFO;
        // Resolve the node id here rather than on first log. EventLogManager builds a NEW logger
        // whenever the record changes, so this always resolves against the record that will be
        // written to. NullEventLogger passes null for both and simply never uses ids.
        if (logrecord != null && logSourceId != null) {
            sourceRef = logrecord.internName(logSourceId);
            idsUsable = sourceRef != LogRecord.NO_ID;
        }
    }

    public EventLogger setLevel(LogLevel level) {
        logLevel = level;
        logLevelValue = level == null ? Integer.MIN_VALUE : level.level;
        logrecord.updateLogLevel(level);
        return this;
    }

    public EventLogger error() {
        logNodeInvocation(LogLevel.ERROR);
        return this;
    }

    public EventLogger warn() {
        logNodeInvocation(LogLevel.WARN);
        return this;
    }

    public EventLogger info() {
        logNodeInvocation(LogLevel.INFO);
        return this;
    }

    public EventLogger debug() {
        logNodeInvocation(LogLevel.DEBUG);
        return this;
    }

    public EventLogger trace() {
        logNodeInvocation(LogLevel.TRACE);
        return this;
    }

    public EventLogger error(String key, String value) {
        log(key, value, LogLevel.ERROR);
        return this;
    }

    public EventLogger warn(String key, String value) {
        log(key, value, LogLevel.WARN);
        return this;
    }

    public EventLogger info(String key, String value) {
        log(key, value, LogLevel.INFO);
        return this;
    }

    public EventLogger debug(String key, String value) {
        log(key, value, LogLevel.DEBUG);
        return this;
    }

    public EventLogger trace(String key, String value) {
        log(key, value, LogLevel.TRACE);
        return this;
    }

    public EventLogger error(String key, boolean value) {
        log(key, value, LogLevel.ERROR);
        return this;
    }

    public EventLogger warn(String key, boolean value) {
        log(key, value, LogLevel.WARN);
        return this;
    }

    public EventLogger error(String key, Object value) {
        log(key, value, LogLevel.ERROR);
        return this;
    }

    public EventLogger warn(String key, Object value) {
        log(key, value, LogLevel.WARN);
        return this;
    }

    public EventLogger info(String key, Object value) {
        log(key, value, LogLevel.INFO);
        return this;
    }

    public EventLogger debug(String key, Object value) {
        log(key, value, LogLevel.DEBUG);
        return this;
    }

    public EventLogger trace(String key, Object value) {
        log(key, value, LogLevel.TRACE);
        return this;
    }

    public EventLogger info(String key, boolean value) {
        log(key, value, LogLevel.INFO);
        return this;
    }

    public EventLogger debug(String key, boolean value) {
        log(key, value, LogLevel.DEBUG);
        return this;
    }

    public EventLogger trace(String key, boolean value) {
        log(key, value, LogLevel.TRACE);
        return this;
    }

    public EventLogger error(String key, double value) {
        log(key, value, LogLevel.ERROR);
        return this;
    }

    public EventLogger warn(String key, double value) {
        log(key, value, LogLevel.WARN);
        return this;
    }

    public EventLogger info(String key, double value) {
        log(key, value, LogLevel.INFO);
        return this;
    }

    public EventLogger debug(String key, double value) {
        log(key, value, LogLevel.DEBUG);
        return this;
    }

    public EventLogger trace(String key, double value) {
        log(key, value, LogLevel.TRACE);
        return this;
    }

    public EventLogger error(String key, int value) {
        log(key, value, LogLevel.ERROR);
        return this;
    }

    public EventLogger warn(String key, int value) {
        log(key, value, LogLevel.WARN);
        return this;
    }

    public EventLogger info(String key, int value) {
        log(key, value, LogLevel.INFO);
        return this;
    }

    public EventLogger debug(String key, long value) {
        log(key, value, LogLevel.DEBUG);
        return this;
    }

    public EventLogger trace(String key, long value) {
        log(key, value, LogLevel.TRACE);
        return this;
    }

    public EventLogger error(String key, long value) {
        log(key, value, LogLevel.ERROR);
        return this;
    }

    public EventLogger warn(String key, long value) {
        log(key, value, LogLevel.WARN);
        return this;
    }

    public EventLogger info(String key, long value) {
        log(key, value, LogLevel.INFO);
        return this;
    }

    public EventLogger debug(String key, int value) {
        log(key, value, LogLevel.DEBUG);
        return this;
    }

    public EventLogger trace(String key, int value) {
        log(key, value, LogLevel.TRACE);
        return this;
    }

    public EventLogger error(String key, char value) {
        log(key, value, LogLevel.ERROR);
        return this;
    }

    public EventLogger warn(String key, char value) {
        log(key, value, LogLevel.WARN);
        return this;
    }

    public EventLogger info(String key, char value) {
        log(key, value, LogLevel.INFO);
        return this;
    }

    public EventLogger debug(String key, char value) {
        log(key, value, LogLevel.DEBUG);
        return this;
    }

    public EventLogger trace(String key, char value) {
        log(key, value, LogLevel.TRACE);
        return this;
    }

    public EventLogger logNodeInvocation(LogLevel logLevel) {
        if (canLog(logLevel)) {
            logrecord.addTrace(logSourceId);
        }
        return this;
    }

    public EventLogger log(String key, Object value, LogLevel logLevel) {
        if (canLog(logLevel)) {
            logrecord.addRecord(logSourceId, key, value);
        }
        return this;
    }

    public EventLogger log(String key, double value, LogLevel logLevel) {
        if (canLog(logLevel)) {
            if (useIds()) {
                logrecord.addRecord(sourceRef, keyRef(key), value);
            } else {
                logrecord.addRecord(logSourceId, key, value);
            }
        }
        return this;
    }

    public EventLogger log(String key, int value, LogLevel logLevel) {
        if (canLog(logLevel)) {
            if (useIds()) {
                logrecord.addRecord(sourceRef, keyRef(key), value);
            } else {
                logrecord.addRecord(logSourceId, key, value);
            }
        }
        return this;
    }

    public EventLogger log(String key, long value, LogLevel logLevel) {
        if (canLog(logLevel)) {
            if (useIds()) {
                logrecord.addRecord(sourceRef, keyRef(key), value);
            } else {
                logrecord.addRecord(logSourceId, key, value);
            }
        }
        return this;
    }

    public EventLogger log(String key, char value, LogLevel logLevel) {
        if (canLog(logLevel)) {
            logrecord.addRecord(logSourceId, key, value);
        }
        return this;
    }

    public EventLogger log(String key, CharSequence value, LogLevel logLevel) {
        if (canLog(logLevel)) {
            logrecord.addRecord(logSourceId, key, value);
        }
        return this;
    }

    public EventLogger log(String key, boolean value, LogLevel logLevel) {
        if (canLog(logLevel)) {
            if (useIds()) {
                logrecord.addRecord(sourceRef, keyRef(key), value);
            } else {
                logrecord.addRecord(logSourceId, key, value);
            }
        }
        return this;
    }

    public boolean canLog(LogLevel logLevel) {
        return logLevelValue >= logLevel.level;
    }
}
