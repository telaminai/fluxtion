/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;
import com.telamin.fluxtion.runtime.event.Event;
import com.telamin.fluxtion.runtime.node.ForkedTrigger;
import com.telamin.fluxtion.runtime.time.Clock;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ObjLongConsumer;

/**
 * Manages and publishes a {@link LogRecord} to a {@link LogRecordListener}. The
 * LogRecord is hydrated from a list of {@link EventLogSource}'s. An
 * EventLogManager configures and supplies a EventLogger instance for each
 * registered EventLogSource, via
 * {@link EventLogSource#setLogger(EventLogger)} com.fluxtion.runtime.plugin.logging.EventLogger)}.
 * The output from each EventLogSource is aggregated into the LogRecord and
 * published.
 * <br>
 * <p>
 * By default all data in the LogRecord is cleared after a publish. Clearing
 * behaviour is controlled with clearAfterPublish flag.
 * <br>
 * <p>
 * EventLogControlEvent events set the logging level for each registered
 * EventLogSource.
 *
 * @author Greg Higgins (greg.higgins@v12technology.com)
 */
public class EventLogManager implements Auditor {

    public static final String NODE_NAME = "eventLogger";
    private LogRecordListener sink;
    private LogRecord logRecord;
    private Map<String, EventLogger> node2Logger;
    private Map<String, EventLogSource> name2LogSourceMap;
    private boolean clearAfterPublish;
    public boolean trace = false;
    public boolean printEventToString = true;
    public boolean printThreadName = true;
    public LogLevel traceLevel;
    @Inject
    public Clock clock;
    private boolean canTrace = false;
    /**
     * Build the binary record at {@link #init()} rather than swapping one in at runtime.
     *
     * <p>Selected by {@code EventProcessorConfig.addLowLatencyEventLog(level, BINARY)}, so it is a
     * build input. The runtime swap through {@code EventLogControlEvent} still works and is still the
     * way to change format on a running processor; this is the way to start in the right one.
     */
    public boolean binaryRecord = false;
    private LogLevel logLevel = LogLevel.INFO;


    public EventLogManager() {
        this(System.out::println);
    }

    public EventLogManager(LogRecordListener sink) {
        if (sink == null) {
            this.sink = l -> {
            };
        } else {
            this.sink = sink;
        }
    }

    public EventLogManager tracingOff() {
        trace = false;
        this.traceLevel = LogLevel.NONE;
        return this;
    }

    public EventLogManager tracingOn(LogLevel level) {
        LogLevel resolvedLevel = level == null ? LogLevel.INFO : level;
        trace = resolvedLevel != LogLevel.NONE;
        this.traceLevel = resolvedLevel;
        return this;
    }

    /**
     * Sets the initial per-node audit entry threshold without enabling method invocation tracing.
     *
     * <p>This is useful for structured business audit entries where the processor should emit
     * {@code EventLogger.info/debug/trace(...)} values but should not record every invoked node and
     * method. Method invocation tracing remains controlled by {@link #tracingOn(LogLevel)}.</p>
     *
     * @param level threshold for {@link EventLogger} entries
     * @return this manager
     */
    public EventLogManager logLevel(LogLevel level) {
        logLevel = level == null ? LogLevel.INFO : level;
        return this;
    }

    public EventLogManager printEventToString(boolean printEventToString) {
        this.printEventToString = printEventToString;
        return this;
    }

    public EventLogManager printThreadName(boolean printThreadName) {
        this.printThreadName = printThreadName;
        return this;
    }

    @Override
    public void nodeRegistered(Object node, String nodeName) {
        EventLogger logger = newLogger(nodeName);
        logger.setLevel(logLevel);
        if (node instanceof EventLogSource) {
            EventLogSource calcSource = (EventLogSource) node;
            calcSource.setLogger(logger);
            name2LogSourceMap.put(nodeName, calcSource);
        }
        node2Logger.put(nodeName, logger);
        canTrace = trace && node2Logger.values().stream().filter(e -> e.canLog(traceLevel)).findAny().isPresent();
    }

    /**
     * The logger every node receives. A {@link BinaryLogRecord} gets a {@link BinaryEventLogger}, which
     * holds the record as a concrete type so the per-entry write is a direct call. No generation is
     * needed for this: nothing about the logger varies per processor.
     */
    private EventLogger newLogger(String nodeName) {
        return logRecord instanceof BinaryLogRecord
                ? new BinaryEventLogger((BinaryLogRecord) logRecord, nodeName)
                : new EventLogger(logRecord, nodeName);
    }

    private void updateLogRecord() {
        for (Map.Entry<String, EventLogSource> stringEventLogSourceEntry : name2LogSourceMap.entrySet()) {
            String nodeName = stringEventLogSourceEntry.getKey();
            EventLogSource calcSource = stringEventLogSourceEntry.getValue();
            EventLogger logger = newLogger(nodeName);
            logger.setLevel(logLevel);
            calcSource.setLogger(logger);
            name2LogSourceMap.put(nodeName, calcSource);
            node2Logger.put(nodeName, logger);
        }
    }

    @Override
    public boolean auditInvocations() {
        return trace;
    }

    @Override
    public void nodeInvoked(Object node, String nodeName, String methodName, Object event) {
        EventLogger logger = node2Logger.getOrDefault(nodeName, NullEventLogger.INSTANCE);
        logger.logNodeInvocation(traceLevel);
        if (printThreadName) {
            logger.log("thread", Thread.currentThread().getName(), traceLevel);
        }
        if (node instanceof ForkedTrigger) {
            logger.log("forkedExecution", "true", traceLevel);
            logger.log("asyncMethod", methodName, traceLevel);
        } else {
            logger.log("method", methodName, traceLevel);
        }
    }

    @OnEventHandler(propagate = false)
    public void calculationLogConfig(EventLogControlEvent newConfig) {
        if (newConfig.getLogRecordProcessor() != null) {
            this.sink = newConfig.getLogRecordProcessor();
        }

        LogRecord newLogRecord = newConfig.getLogRecord();
        if (newLogRecord != null) {
            newLogRecord.updateLogLevel(logRecord.getLogLevel());
            newLogRecord.replaceBuffer(logRecord.sb);
            this.logRecord = newLogRecord;
            this.logRecord.setClock(clock);
            updateLogRecord();
        }

        final LogLevel level = newConfig.getLevel();
        if (level != null
                && (logRecord.groupingId == null || logRecord.groupingId.equals(newConfig.getGroupId()))) {
//            LOGGER.log(Level.INFO, "updating event log config:{0}", newConfig);
            System.out.println("updating event log config:" + newConfig);
            node2Logger.computeIfPresent(newConfig.getSourceId(), (t, u) -> {
                u.setLevel(level);
                return u;
            });
            if (newConfig.getSourceId() == null) {
                node2Logger.values().forEach((t) -> t.setLevel(newConfig.getLevel()));
                logLevel = newConfig.getLevel();
            }
        }

        final ObjLongConsumer<StringBuilder> timeFormatter = newConfig.getTimeFormatter();
        if (timeFormatter != null) {
            logRecord.setTimeFormatter(timeFormatter);
        }

        canTrace = trace && node2Logger.values().stream().filter(e -> e.canLog(traceLevel)).findAny().isPresent();
    }

    /** Visible for tests: confirms {@link #init()} built the format the profile asked for. */
    public boolean lastRecordIsBinaryForTest() {
        return logRecord instanceof BinaryLogRecord;
    }

    public void setLogSink(LogRecordListener sink) {
        this.sink = sink;
    }

    public void setLogGroupId(String groupId) {
        logRecord.groupingId = groupId;
    }

    public void setClearAfterPublish(boolean clearAfterPublish) {
        this.clearAfterPublish = clearAfterPublish;
    }

    /**
     * makes best efforts to dump the current {@link LogRecord} to the registered sink. Useful when error handling
     * if an exception is thrown
     */
    public void publishLastRecord() {
        logRecord.terminateRecord();
        sink.processLogRecord(logRecord);
        logRecord.clear();
    }

    /**
     * makes best efforts to dump the current {@link LogRecord} to as a String. Useful when error handling
     * if an exception is thrown
     *
     * @return The lates {@link LogRecord} as a String
     */
    public String lastRecordAsString() {
        return logRecord.toString();
    }

    @Override
    public void processingComplete() {
        if (canTrace | logRecord.terminateRecord()) {
            sink.processLogRecord(logRecord);
        }
        if (clearAfterPublish) {
            logRecord.clear();
        }
    }

    /**
     * Build a {@link BinaryLogRecord} at {@link #init()} instead of the text record. Set by
     * {@code EventProcessorConfig.addLowLatencyEventLog(level, BINARY)} so the format is a build input.
     */
    public EventLogManager binaryRecord(boolean binaryRecord) {
        this.binaryRecord = binaryRecord;
        return this;
    }

    @Override
    public void init() {
        logRecord = binaryRecord ? new BinaryLogRecord(clock) : new LogRecord(clock);
        logRecord.printEventToString(printEventToString);
        logRecord.setPrintThreadName(printThreadName);
        node2Logger = new HashMap<>();
        name2LogSourceMap = new HashMap<>();
        clearAfterPublish = true;
    }

    @Override
    public void eventReceived(Event triggerEvent) {
        logRecord.triggerEvent(triggerEvent);
    }

    @Override
    public void eventReceived(Object triggerEvent) {
        logRecord.triggerObject(triggerEvent);
    }

}
