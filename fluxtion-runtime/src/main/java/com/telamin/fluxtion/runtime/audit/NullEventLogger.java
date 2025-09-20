/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;

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
