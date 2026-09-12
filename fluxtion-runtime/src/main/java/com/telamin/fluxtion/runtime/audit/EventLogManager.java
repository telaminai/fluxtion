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
    /**
     * Whether the record takes a second clock reading for {@code endTime}. Default true - every
     * release has emitted {@code endTime}. {@code LOW_LATENCY_AUDIT} sets it false: that second
     * reading is 13.6 ns of a 37 ns audited event on the accurate default clock, and it exists only for
     * {@code endTime - logTime}. A generated processor carries this as a field assignment, so the
     * profile decision made at build time is the one the record runs under.
     *
     * <p><b>Precedence.</b> This setting governs the records the manager BUILDS (at {@link #init()}) and
     * the record it currently holds when the setter is called; a record the caller SUPPLIES through
     * {@link EventLogControlEvent} keeps its own {@code setRecordEndTime}, because an explicit record
     * is an explicit choice and a supplied record's default is true, as every release wrote it. On the wire an unrecorded
     * {@code endTime} is 0, which the format defines as "not recorded" and the analyser reads as absent.
     */
    public boolean recordEndTime = true;
    private LogLevel logLevel = LogLevel.INFO;


    public EventLogManager() {
        this(DEFAULT_SINK);
    }

    /**
     * The implicit sink: prints text records, and REFUSES a binary one by name.
     *
     * <p>Printing a {@link BinaryLogRecord} used to call {@code toString()} ->
     * {@code asCharSequence()}, which throws {@code UnsupportedOperationException} by design because a
     * binary record has no character form. That named neither the sink nor the format that chose it.
     *
     * <p><b>The check lives here rather than in {@link #init()}.</b> An earlier fix refused at init and
     * broke every generated processor: generated code calls {@code EventLogManager.init()} from the
     * PROCESSOR'S CONSTRUCTOR, so the guard fired while the processor was still being built — before
     * any caller could retrieve the auditor and install a sink. The documented sequence is construct,
     * retrieve the auditor, install the writer, then {@code processor.init()}; refusing at auditor init
     * closed that window. Refusing at first publish keeps it open and still names the fix.
     */
    private static final LogRecordListener DEFAULT_SINK = logRecord -> {
        if (logRecord instanceof BinaryLogRecord) {
            throw new IllegalStateException(
                    "binary audit records were selected but no sink was installed to receive them.\n"
                            + "The default sink prints records as text, and a binary record has no text "
                            + "form.\n"
                            + "Install a sink that takes bytes, after the processor is constructed and "
                            + "before it processes events, for example:\n"
                            + "    EventLogManager m = processor.getAuditorById(EventLogManager.NODE_NAME);\n"
                            + "    m.setLogSink(new BinaryLogWriter(Files.newOutputStream(path)));\n"
                            + "Use AuditRecordFormat.TEXT if you want records on the default sink.");
        }
        System.out.println(logRecord);
    };

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
            // PRECEDENCE: the manager's recordEndTime governs the records the MANAGER builds; a record
            // the caller supplies keeps its own setting. An earlier version overwrote it, so a caller
            // who had chosen setRecordEndTime(false) on a replacement record - the route the docs
            // recommend for any other profile - had the choice silently undone (review, round 9).
            // A supplied record's default is true, as every release wrote endTime; a caller under
            // LOW_LATENCY_AUDIT who swaps in a record and wants it off sets it on that record.
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

    /**
     * @see #recordEndTime
     * <p>LIVE: applies to the record this manager currently holds as well as to the ones it will
     * build. A generated processor constructs its manager and record before any user code runs, so
     * a setter that updated only the field left the active record unchanged until the next swap
     * (review, round 9). Precedence: this setting governs the records the manager builds; a record
     * the caller supplies through {@link EventLogControlEvent} keeps its own.
     */
    public EventLogManager recordEndTime(boolean recordEndTime) {
        this.recordEndTime = recordEndTime;
        if (logRecord != null) {
            logRecord.setRecordEndTime(recordEndTime);
        }
        return this;
    }

    @Override
    public void init() {
        logRecord = binaryRecord ? new BinaryLogRecord(clock) : new LogRecord(clock);
        logRecord.printEventToString(printEventToString);
        logRecord.setPrintThreadName(printThreadName);
        logRecord.setRecordEndTime(recordEndTime);
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
