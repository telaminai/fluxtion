/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;

/**
 * An {@link EventLogger} that holds its record as a <b>concrete</b> {@link BinaryLogRecord}, so the
 * per-entry write is a direct call rather than a virtual dispatch through {@link LogRecord}.
 *
 * <h2>Why this is in the runtime and not generated</h2>
 * An earlier design proposed generating this class into each processor. It does not need to be
 * generated, because <b>nothing about it varies per processor</b>:
 * <ul>
 *   <li>the node id is a <em>constructor argument</em>, so it varies per instance, not per class;</li>
 *   <li>the record type is {@link BinaryLogRecord} for every binary processor;</li>
 *   <li>{@link BinaryLogRecord} is {@code final}, so a field of that type devirtualises here exactly as
 *       it would in generated code.</li>
 * </ul>
 *
 * <p>And generating it would not have bought what it was thought to buy. The value stores live inside
 * {@link BinaryLogRecord}, which is core and targets Java 8, so a generated logger calling
 * {@code addRecord} still reaches the byte loop — the {@code VarHandle} path is not reachable by
 * generating the <em>logger</em>. Reaching it needs the stores themselves to move into generated code,
 * which is a different and more invasive design.
 *
 * <p>So the common case costs no generated classes at all, and the code-cache question does not arise.
 */
public final class BinaryEventLogger extends EventLogger {

    private final BinaryLogRecord record;

    public BinaryEventLogger(BinaryLogRecord record, String logSourceId) {
        super(record, logSourceId);
        this.record = record;
    }

    @Override
    public EventLogger log(String key, double value, LogLevel logLevel) {
        if (canLog(logLevel) && useIds()) {
            record.addRecord(sourceRef, keyRef(key), value);
            return this;
        }
        return super.log(key, value, logLevel);
    }

    @Override
    public EventLogger log(String key, long value, LogLevel logLevel) {
        if (canLog(logLevel) && useIds()) {
            record.addRecord(sourceRef, keyRef(key), value);
            return this;
        }
        return super.log(key, value, logLevel);
    }

    @Override
    public EventLogger log(String key, int value, LogLevel logLevel) {
        if (canLog(logLevel) && useIds()) {
            record.addRecord(sourceRef, keyRef(key), value);
            return this;
        }
        return super.log(key, value, logLevel);
    }

    @Override
    public EventLogger log(String key, boolean value, LogLevel logLevel) {
        if (canLog(logLevel) && useIds()) {
            record.addRecord(sourceRef, keyRef(key), value);
            return this;
        }
        return super.log(key, value, logLevel);
    }
}
