/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.audit;

import com.fluxtion.dataflow.runtime.audit.EventLogControlEvent.LogLevel;

/**
 * No operation logger, has no side effects on any function call, ie no logging
 *
 * @author gregp
 */
public final class NullEventLogger extends EventLogger {

    public static final NullEventLogger INSTANCE = new NullEventLogger();

    private NullEventLogger() {
        super(null, null);
    }

    @Override
    public EventLogger log(String key, boolean value, LogLevel logLevel) {
        return null;
    }

    @Override
    public EventLogger log(String key, CharSequence value, LogLevel logLevel) {
        return null;
    }

    @Override
    public EventLogger log(String key, int value, LogLevel logLevel) {
        return this;
    }

    @Override
    public EventLogger log(String key, long value, LogLevel logLevel) {
        return null;
    }


    @Override
    public EventLogger log(String key, double value, LogLevel logLevel) {
        return this;
    }

    @Override
    public EventLogger log(String key, char value, LogLevel logLevel) {
        return null;
    }

    @Override
    public EventLogger log(String key, Object value, LogLevel logLevel) {
        return this;
    }

    @Override
    public EventLogger logNodeInvocation(LogLevel logLevel) {
        return this;
    }

}
