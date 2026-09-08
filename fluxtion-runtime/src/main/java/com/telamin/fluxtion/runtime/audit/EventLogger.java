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
     * Ids resolved once against {@link #logrecord}, if it uses them. {@code logSourceId} is final, so
     * the node's id never changes; keys are a tiny reference-compared cache because a node logs a small
     * fixed set of them. No hashing, no map — the measurement that motivated this showed the cost was
     * performing a lookup at all, so the fast path must be a reference compare and nothing more.
     */
    protected int sourceRef = LogRecord.NO_ID;
    private boolean idsResolved;
    private boolean idsUsable;
    private static final int KEY_SLOTS = 4;
    private final String[] keyNames = new String[KEY_SLOTS];
    private final int[] keyRefs = new int[KEY_SLOTS];
    private int keyCount;

    /**
     * Whether this logger's record accepts integer ids. Exposed to subclasses so a record-specialised
     * logger can take the id path without duplicating the resolution.
     */
    /**
     * Keys this logger's node can log, declared once, indexed by ordinal.
     *
     * <p>The source-level form of what a code model or annotation processor would generate: seeing
     * {@code auditLog.info("price", price)} in node source it knows both the node and the key, and can
     * emit {@code auditLog.info(0, price)} against a declared key list. The runtime then resolves each
     * key to a record id <b>once</b> instead of comparing a {@code String} reference per call.
     *
     * <p>No bytecode rewriting and no vendor-jar rewriting, so the generated source remains the code
     * that runs — the property an ASM transform would have cost.
     */
    private String[] declaredKeys;
    private int[] declaredKeyRefs;
    private static final int UNRESOLVED = LogRecord.NO_ID - 1;

    /** Declare the keys this node logs, in the order its ordinal calls use. */
    public EventLogger declareKeys(String... keys) {
        this.declaredKeys = keys;
        this.declaredKeyRefs = new int[keys.length];
        java.util.Arrays.fill(this.declaredKeyRefs, UNRESOLVED);
        return this;
    }

    protected int ordinalRef(int ordinal) {
        int ref = declaredKeyRefs[ordinal];
        if (ref != UNRESOLVED) {
            return ref;
        }
        ref = logrecord.internName(declaredKeys[ordinal]);
        declaredKeyRefs[ordinal] = ref;
        return ref;
    }

    /** Ordinal-indexed logging at INFO; the key is an index into {@link #declareKeys}. */
    public EventLogger info(int keyOrdinal, double value) {
        return log(keyOrdinal, value, LogLevel.INFO);
    }

    public EventLogger info(int keyOrdinal, long value) {
        return log(keyOrdinal, value, LogLevel.INFO);
    }

    public EventLogger log(int keyOrdinal, double value, LogLevel logLevel) {
        if (this.logLevel.level >= logLevel.level) {
            if (useIds()) {
                logrecord.addRecord(sourceRef, ordinalRef(keyOrdinal), value);
            } else {
                // A record with no id space still gets the entry. An ordinal is a way of NAMING a key,
                // not a second wire format, so a record that declines ids must see the same write it
                // would have seen from the String API rather than silently lose it.
                logrecord.addRecord(logSourceId, declaredKeys[keyOrdinal], value);
            }
        }
        return this;
    }

    public EventLogger log(int keyOrdinal, long value, LogLevel logLevel) {
        if (this.logLevel.level >= logLevel.level) {
            if (useIds()) {
                logrecord.addRecord(sourceRef, ordinalRef(keyOrdinal), value);
            } else {
                // A record with no id space still gets the entry. An ordinal is a way of NAMING a key,
                // not a second wire format, so a record that declines ids must see the same write it
                // would have seen from the String API rather than silently lose it.
                logrecord.addRecord(logSourceId, declaredKeys[keyOrdinal], value);
            }
        }
        return this;
    }

    protected boolean useIds() {
        if (!idsResolved) {
            idsResolved = true;
            sourceRef = logrecord.internName(logSourceId);
            idsUsable = sourceRef != LogRecord.NO_ID;
        }
        return idsUsable;
    }

    /** The id for a property key, resolved once and cached by reference. Subclass-visible for the same
     *  reason as {@link #useIds()}. */
    protected int keyRef(String key) {
        for (int i = 0; i < keyCount; i++) {
            if (keyNames[i] == key) {          // identity: keys are literals, so interned constants
                return keyRefs[i];
            }
        }
        int ref = logrecord.internName(key);
        if (keyCount < KEY_SLOTS) {
            keyNames[keyCount] = key;
            keyRefs[keyCount] = ref;
            keyCount++;
        }
        return ref;
    }

    public EventLogger(LogRecord logrecord, String logSourceId) {
        this.logrecord = logrecord;
        this.logSourceId = logSourceId;
        logLevel = LogLevel.INFO;
    }

    public EventLogger setLevel(LogLevel level) {
        logLevel = level;
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
        if (this.logLevel.level >= logLevel.level) {
            logrecord.addTrace(logSourceId);
        }
        return this;
    }

    public EventLogger log(String key, Object value, LogLevel logLevel) {
        if (this.logLevel.level >= logLevel.level) {
            logrecord.addRecord(logSourceId, key, value);
        }
        return this;
    }

    public EventLogger log(String key, double value, LogLevel logLevel) {
        if (this.logLevel.level >= logLevel.level) {
            if (useIds()) {
                logrecord.addRecord(sourceRef, keyRef(key), value);
            } else {
                logrecord.addRecord(logSourceId, key, value);
            }
        }
        return this;
    }

    public EventLogger log(String key, int value, LogLevel logLevel) {
        if (this.logLevel.level >= logLevel.level) {
            if (useIds()) {
                logrecord.addRecord(sourceRef, keyRef(key), value);
            } else {
                logrecord.addRecord(logSourceId, key, value);
            }
        }
        return this;
    }

    public EventLogger log(String key, long value, LogLevel logLevel) {
        if (this.logLevel.level >= logLevel.level) {
            if (useIds()) {
                logrecord.addRecord(sourceRef, keyRef(key), value);
            } else {
                logrecord.addRecord(logSourceId, key, value);
            }
        }
        return this;
    }

    public EventLogger log(String key, char value, LogLevel logLevel) {
        if (this.logLevel.level >= logLevel.level) {
            logrecord.addRecord(logSourceId, key, value);
        }
        return this;
    }

    public EventLogger log(String key, CharSequence value, LogLevel logLevel) {
        if (this.logLevel.level >= logLevel.level) {
            logrecord.addRecord(logSourceId, key, value);
        }
        return this;
    }

    public EventLogger log(String key, boolean value, LogLevel logLevel) {
        if (this.logLevel.level >= logLevel.level) {
            if (useIds()) {
                logrecord.addRecord(sourceRef, keyRef(key), value);
            } else {
                logrecord.addRecord(logSourceId, key, value);
            }
        }
        return this;
    }

    public boolean canLog(LogLevel logLevel) {
        return this.logLevel != null && this.logLevel.level >= logLevel.level;
    }
}
