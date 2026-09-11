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

    /**
     * Which clock the AUDIT RECORD's timestamps come from. Build input; {@code SHARED} by default.
     *
     * @see #auditClock(AuditClock)
     */
    public AuditClock auditClock = AuditClock.SHARED;

    /**
     * The private clock, built at {@link #init()} when {@link #auditClock} is not {@code SHARED}.
     * Driven by this manager's own auditor callbacks, so it never touches the graph's clock.
     */
    private transient Clock privateAuditClock;
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


    /**
     * TRUE while the sink is the implicit {@code System.out::println} nobody asked for.
     *
     * <p>Needed because a println sink and a binary record are incompatible in a way that used to
     * surface as an {@code UnsupportedOperationException} from {@code toString()} on the first
     * published record — deep in the runtime, long after the build that chose BINARY. See {@link #init()}.
     */
    private boolean sinkIsImplicitDefault = false;

    public EventLogManager() {
        this(System.out::println);
        this.sinkIsImplicitDefault = true;
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
            // recordClock(), not clock: a record swapped in at runtime must read the same clock the
            // build chose, or the timestamps change source halfway through a log.
            this.logRecord.setClock(recordClock());
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
        this.sinkIsImplicitDefault = false;
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
    /**
     * Where the audit record's {@code logTime} and {@code endTime} come from.
     *
     * <p>The graph's {@link Clock} is a process-wide singleton the generator injects into every
     * processor, and time-based nodes read it — {@code FixedRateTrigger.atMillis}, tumbling and sliding
     * windows. Its strategy is therefore not the audit path's to change: swapping it for a cheaper one
     * to save a clock read per audited event also changes what every window in the graph believes the
     * time is. That is why a PROFILE could not select a fast clock, and why this is a separate clock
     * rather than a strategy on the shared one.
     */
    public enum AuditClock {
        /**
         * The graph's clock. <b>Default.</b> One clock in the system, so an audit timestamp and a
         * window's idea of now cannot disagree.
         */
        SHARED,
        /**
         * A private {@link com.telamin.fluxtion.runtime.time.ClockStrategy#fastEpochMillisClock()}
         * for the audit record only, leaving the graph's clock alone.
         *
         * <p>Saves the difference between a {@code currentTimeMillis} call and a {@code nanoTime} one
         * on every audited event — ~4.9 ns measured on an Apple M4 — and the graph's time-based nodes
         * are unaffected.
         *
         * <p><b>The cost is in the log, and it is the reason this is not the default.</b> A projected
         * clock anchors once and never sees a later NTP or manual wall-clock correction, so every
         * {@code logTime} written after a correction is on the old timeline, and the drift accumulates
         * for the life of the process. The graph's clock stays right while the LOG goes wrong, which is
         * the wrong way round for a record whose job is saying when things happened. Choose it when
         * nothing correlates these timestamps with anything outside the JVM.
         */
        FAST_PROJECTED
    }

    /** @see AuditClock */
    public EventLogManager auditClock(AuditClock auditClock) {
        this.auditClock = auditClock == null ? AuditClock.SHARED : auditClock;
        return this;
    }

    /** The clock the record reads: the private one when configured, the graph's otherwise. */
    private Clock recordClock() {
        return privateAuditClock == null ? clock : privateAuditClock;
    }

    public EventLogManager binaryRecord(boolean binaryRecord) {
        this.binaryRecord = binaryRecord;
        return this;
    }

    @Override
    public void init() {
        // REFUSE AT INIT, not at the first published record. A binary record cannot go to the implicit
        // System.out::println sink: publishing calls toString(), which calls asCharSequence(), which
        // throws by design because a binary record has no character form. That surfaced as an
        // UnsupportedOperationException several frames inside the runtime, naming neither the sink nor
        // the format that chose it - and only once an event had been processed, so a build and a start
        // both looked fine.
        //
        // Choosing BINARY is a build input; installing somewhere for the bytes to go is not optional,
        // and there is no safe default: binary framing written to a terminal as text is not a usable
        // log. So the requirement is stated here, where it can name the fix.
        if (binaryRecord && sinkIsImplicitDefault) {
            throw new IllegalStateException(
                    "binary audit records were selected but no sink was installed to receive them.\n"
                            + "The default sink prints records as text, and a binary record has no text "
                            + "form - publishing one would throw inside the event cycle.\n"
                            + "Install a sink that takes bytes before init(), for example:\n"
                            + "    manager.setLogSink(new BinaryLogWriter(Files.newOutputStream(path)));\n"
                            + "where manager is the EventLogManager auditor - "
                            + "processor.getAuditorById(EventLogManager.NODE_NAME).\n"
                            + "Use AuditRecordFormat.TEXT if you want records on the default sink.");
        }
        if (auditClock == AuditClock.FAST_PROJECTED) {
            privateAuditClock = new Clock();
            privateAuditClock.init();
            privateAuditClock.setClockStrategy(
                    new com.telamin.fluxtion.runtime.time.ClockStrategy.ClockStrategyEvent(
                            com.telamin.fluxtion.runtime.time.ClockStrategy.fastEpochMillisClock()));
        } else {
            privateAuditClock = null;
        }
        Clock recordClock = recordClock();
        logRecord = binaryRecord ? new BinaryLogRecord(recordClock) : new LogRecord(recordClock);
        logRecord.printEventToString(printEventToString);
        logRecord.setPrintThreadName(printThreadName);
        node2Logger = new HashMap<>();
        name2LogSourceMap = new HashMap<>();
        clearAfterPublish = true;
    }

    @Override
    public void eventReceived(Event triggerEvent) {
        if (privateAuditClock != null) {
            privateAuditClock.eventReceived(triggerEvent);
        }
        logRecord.triggerEvent(triggerEvent);
    }

    @Override
    public void eventReceived(Object triggerEvent) {
        if (privateAuditClock != null) {
            privateAuditClock.eventReceived(triggerEvent);
        }
        logRecord.triggerObject(triggerEvent);
    }

}
