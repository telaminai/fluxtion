/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.replay;

import com.telamin.fluxtion.builder.validation.BaseEventProcessorRowBasedTest;
import com.telamin.fluxtion.runtime.CloneableDataFlow;
import com.telamin.fluxtion.runtime.event.ReplayRecord;

import java.io.Reader;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility for replaying {@link ReplayRecord}'s into an {@link CloneableDataFlow}. ReplayRecord's in the form of yaml are
 * read using a supplied reader. Supports:
 *
 * <ul>
 *     <li>Optionally call init on the CloneableDataFlow under test</li>
 *     <li>Optionally call start on the CloneableDataFlow under test</li>
 *     <li>Optionally set a time range for records to push to the CloneableDataFlow under test</li>
 *     <li>Optionally set a minimum start time for records to push to the CloneableDataFlow under test</li>
 *     <li>Optionally set a maximum end time for records to push to the CloneableDataFlow under test</li>
 * </ul>
 * <p>
 * Sample usage for between times, calling lifecycle methods init and start:
 * <pre>
 *  CloneableDataFlow eventProcessor = new MyEventProcessor();
 *  YamlReplayRunner.newSession(new StringReader(replayEventLog), eventProcessor)
 *         .betweenTimes(600, 680)
 *         .callInit()
 *         .callStart()
 *         .runReplay();
 * </pre>
 */
public class YamlReplayRunner {

    private final Reader yamlReplayRecordSource;
    private final CloneableDataFlow<?> eventProcessor;
    private long minimumTime = Long.MIN_VALUE;
    private long maximumTime = Long.MAX_VALUE;

    private YamlReplayRunner(Reader yamlReplayRecordSource, CloneableDataFlow<?> eventProcessor) {
        this.yamlReplayRecordSource = yamlReplayRecordSource;
        this.eventProcessor = eventProcessor;
    }

    public static YamlReplayRunner newSession(Reader yamlReplayRecordSource, CloneableDataFlow<?> eventProcessor) {
        return new YamlReplayRunner(yamlReplayRecordSource, eventProcessor);
    }

    public YamlReplayRunner callInit() {
        eventProcessor.init();
        return this;
    }

    public YamlReplayRunner callStart() {
        eventProcessor.start();
        return this;
    }

    public YamlReplayRunner startComplete() {
        eventProcessor.startComplete();
        return this;
    }

    public YamlReplayRunner afterTime(long startTime) {
        return betweenTimes(startTime, Long.MAX_VALUE);
    }

    public YamlReplayRunner beforeTime(long stopTime) {
        return betweenTimes(Long.MIN_VALUE, stopTime);
    }

    public YamlReplayRunner betweenTimes(long startTime, long stopTime) {
        minimumTime = startTime;
        maximumTime = stopTime;
        return this;
    }

    public void runReplay() {
        AtomicLong timeSupplier = new AtomicLong();
        eventProcessor.setClockStrategy(timeSupplier::get);
        //run the audit log setting the clock time programmatically from the replay time
        BaseEventProcessorRowBasedTest.yamlToStream(yamlReplayRecordSource, ReplayRecord.class)
                .forEachOrdered(t -> {
                    long wallClockTime = t.getWallClockTime();
                    if (wallClockTime < maximumTime && wallClockTime > minimumTime) {
                        timeSupplier.set(wallClockTime);
                        eventProcessor.onEvent(t.getEvent());
                    }
                });
    }
}
