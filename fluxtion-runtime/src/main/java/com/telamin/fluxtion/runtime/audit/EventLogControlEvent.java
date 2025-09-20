/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.event.Event;
import lombok.Getter;

import java.util.function.ObjLongConsumer;

/**
 * Control message to manage the audit logging of events by the
 * {@link EventLogManager} at runtime. Configurable options:
 * <ul>
 * <li> Log level of an EventLogSource instance with {@link #sourceId}.
 * <li> Log level of a group EventLogSource with {@link  #groupId}.
 * <li> Log level of all EventLogSource instances.
 * <li> The LogRecordListener to process LogRecords.
 * </ul>
 *
 * @author Greg Higgins (greg.higgins@v12technology.com)
 */
public class EventLogControlEvent implements Event {

    @Getter
    private LogLevel level;
    /**
     * The name of the node to apply the configuration to. A null value, the
     * default, is no filtering and configuration will be applied to all nodes.
     */
    @Getter
    private String sourceId;
    /**
     * The group Id of a SEP to apply the configuration to. A null value, the
     * default, is no filtering and configuration will be applied to all SEP's.
     */
    @Getter
    private String groupId;
    /**
     * user configured {@link LogRecordListener}
     */
    @Getter
    private LogRecordListener logRecordProcessor;
    /**
     * User configured {@link LogRecord}
     */
    @Getter
    private LogRecord logRecord;
    /**
     * Custom time formatter
     */
    @Getter
    private ObjLongConsumer<StringBuilder> timeFormatter;

    public EventLogControlEvent() {
        this(LogLevel.INFO);
    }

    public EventLogControlEvent(LogLevel level) {
        this(null, null, level);
    }

    public EventLogControlEvent(LogRecordListener logRecordProcessor) {
        this(null, null, null);
        this.logRecordProcessor = logRecordProcessor;
    }

    public EventLogControlEvent(LogRecord logRecord) {
        this(null, null, null);
        this.logRecord = logRecord;
    }

    public EventLogControlEvent(ObjLongConsumer<StringBuilder> timeFormatter) {
        this(null, null, null);
        this.timeFormatter = timeFormatter;
    }

    public EventLogControlEvent(String sourceId, String groupId, LogLevel level) {
        this.sourceId = sourceId;
        this.groupId = groupId;
        this.level = level;
    }

    public EventLogControlEvent(String sourceId, String groupId, LogLevel level, LogRecordListener logRecordProcessor) {
        this.sourceId = sourceId;
        this.groupId = groupId;
        this.logRecordProcessor = logRecordProcessor;
        this.level = level;
    }

    public enum LogLevel {
        NONE(0), ERROR(1), WARN(2), INFO(3), DEBUG(4), TRACE(5);

        private LogLevel(int level) {
            this.level = level;
        }

        public final int level;
    }

    @Override
    public String toString() {
        return "EventLogConfig{"
                + "level=" + level
                + ", logRecordProcessor=" + logRecordProcessor
                + ", sourceId=" + sourceId
                + ", groupId=" + groupId
                + '}';
    }

}
