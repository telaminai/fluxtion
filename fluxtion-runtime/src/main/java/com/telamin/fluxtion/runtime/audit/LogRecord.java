/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.event.Event;
import com.telamin.fluxtion.runtime.time.Clock;
import lombok.Getter;
import lombok.Setter;

import java.util.function.ObjLongConsumer;

/**
 * A structured log record that can be easily converted to a long term store,
 * such as a rdbms for later analysis. The LogRecord creates a yaml
 * representation of the LogRecord for simplified marshaling.
 * <br>
 * <p>
 * A log record is triggered when an event is processed and written at the end
 * of the execution loop. A sample record:
 *
 * <pre>
 * eventLogRecord:
 *   eventTime: 1578236915413
 *   logTime: 1578236915413
 *   groupingId: null
 *   event: CharEvent
 *   nodeLogs:
 *     - parentNode_1: { char: a, prop: 32}
 *     - childNode_3: { child: true}
 *   endTime: 1578236915413
 * <pre>
 * <ul>
 * <li>eventTime: the time the event was created
 * <li>logTime: the time the log record is created i.e. when the event processing began
 * <li>endTime: the time the log record is complete i.e. when the event processing completed
 * <li>groupingId: a set of nodes can have a groupId and their configuration is controlled as a group
 * <li>event: The simple class name of the event that created the execution
 * <li>nodeLogs: are recorded in the order the event wave passes through the graph.
 * Only nodes that on the execution path have an entry in the nodeList element.
 * The key of a node is the instance name in the java code, followed by a set of key/value properties the node logs.
 * </ul>
 *
 * @author Greg Higgins (greg.higgins@v12technology.com)
 */
public class LogRecord {

    /**
     * The id of the instance producing the record. GroupingId can be used to
     * group LogRecord's together.
     */
    public String groupingId;
    @Getter
    private EventLogControlEvent.LogLevel logLevel;
    protected final StringBuilder sb;
    protected String sourceId;
    protected boolean firstProp;
    /**
     * Whether to take a second clock reading for {@code endTime}. <b>Off by default.</b>
     *
     * <p>{@code endTime} exists so {@code endTime - logTime} gives the processing duration, and it has
     * to be a live reading to do that — a cached one reports every event as taking zero time. But that
     * costs a clock read on every event, and on any event faster than the clock's resolution the answer
     * is zero anyway. An audited event path reads a clock twice per event; this is the second one, and
     * most deployments do not need it.
     *
     * <p>Enable it when you actually consume the duration, and pair it with a clock that can resolve it:
     * {@link com.telamin.fluxtion.runtime.time.ClockStrategy#nanoEpochClock()}.
     */
    protected boolean recordEndTime = false;
    @Setter
    protected Clock clock;
    protected boolean printEventToString = false;
    @Setter
    protected boolean printThreadName = false;
    @Setter
    protected ObjLongConsumer<StringBuilder> timeFormatter = StringBuilder::append;

    /**
     * Writes this record's ENCODED form, so a sink can persist it without knowing its class.
     *
     * <p>M52.3, spec-binary-audit-encoding §6.1(2). Before this, {@code asCharSequence()} was the only
     * expression channel: a binary record had to throw from it, and every sink that wanted the bytes
     * had to downcast to a vendor class to reach them. A sink shipping records to a queue or a socket
     * has no business knowing whether the graph was built with a text or a binary log.
     *
     * <p>The default is the text answer the spec names — the characters, as UTF-8 — so every existing
     * {@code LogRecord} subclass satisfies the contract without changing. A binary record overrides it
     * with its own framing.
     *
     * <p><b>Stream-level framing is NOT a record's business.</b> A binary log file also carries a
     * header and dictionary frames, and which names a stream has already described is state belonging
     * to the writer, not to any one record. {@link BinaryLogWriter} still owns that;
     * this is the record's own bytes.
     */
    public void encodeTo(java.io.OutputStream out) throws java.io.IOException {
        CharSequence text = asCharSequence();
        if (text != null) {
            out.write(text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    public LogRecord(Clock clock) {
        this(clock, EventLogControlEvent.LogLevel.INFO);
    }

    public LogRecord(Clock clock, EventLogControlEvent.LogLevel logLevel) {
        sb = new StringBuilder();
        firstProp = true;
        this.clock = clock;
        updateLogLevel(logLevel);
    }

    public void updateLogLevel(EventLogControlEvent.LogLevel newLogLevel) {
        this.logLevel = newLogLevel == null ? EventLogControlEvent.LogLevel.NONE : newLogLevel;
        if (!loggingEnabled()) {
            sb.setLength(0);
        }
    }

    /** @see #recordEndTime */
    public void setRecordEndTime(boolean recordEndTime) {
        this.recordEndTime = recordEndTime;
    }

    public boolean isRecordEndTime() {
        return recordEndTime;
    }

    public void replaceBuffer(CharSequence newBuffer) {
        sb.setLength(0);
        sb.append(newBuffer);
    }

    /**
     * Resolve a node name or property key to a small integer this record can use in place of the
     * string, or return {@link #NO_ID} to say it does not work that way.
     *
     * <p><b>Why this exists.</b> Every name reaching {@code addRecord} is a compile-time constant: the
     * node name comes from the generated processor, the property key is a literal in the node's own
     * source. A record that encodes names as integers must therefore resolve the same handful of
     * strings on every event forever. Measured on a 30-node graph logging 11.75 entries per event, that
     * is 23.5 lookups and <b>26 ns/event on JIT, 27 on native</b> — about a third of a binary audit
     * record, and an open-addressed identity table recovered only 7% of it, because the cost is doing a
     * lookup at all rather than which lookup.
     *
     * <p>{@link EventLogger} is created per node and holds its node name in a final field, so it can
     * resolve once and reuse. This hook is what lets it. <b>Node code does not change</b> —
     * {@code auditLog.info("v", v)} is unaffected.
     *
     * @param name a node name or property key, always a constant in practice
     * @return a non-negative id, or {@link #NO_ID} if this record encodes names directly
     */
    public int internName(String name) {
        return NO_ID;
    }

    /** Returned by {@link #internName} when a record does not use integer ids. */
    public static final int NO_ID = -1;

    /**
     * The id-carrying counterparts of the {@code addRecord} overloads. A record that returns real ids
     * from {@link #internName} MUST override the ones it can receive; the defaults delegate nowhere and
     * exist so that adding this to the API breaks no existing subclass.
     */
    public void addRecord(int sourceRef, int keyRef, double value) {
        throw new UnsupportedOperationException("record returned ids from internName but did not "
                + "override addRecord(int, int, double)");
    }

    public void addRecord(int sourceRef, int keyRef, long value) {
        throw new UnsupportedOperationException("record returned ids from internName but did not "
                + "override addRecord(int, int, long)");
    }

    public void addRecord(int sourceRef, int keyRef, int value) {
        throw new UnsupportedOperationException("record returned ids from internName but did not "
                + "override addRecord(int, int, int)");
    }

    public void addRecord(int sourceRef, int keyRef, boolean value) {
        throw new UnsupportedOperationException("record returned ids from internName but did not "
                + "override addRecord(int, int, boolean)");
    }

    public void addRecord(String sourceId, String propertyKey, double value) {
        addSourceId(sourceId, propertyKey);
        sb.append(value);
    }

    public void addRecord(String sourceId, String propertyKey, long value) {
        addSourceId(sourceId, propertyKey);
        sb.append(value);
    }

    public void addRecord(String sourceId, String propertyKey, int value) {
        addSourceId(sourceId, propertyKey);
        sb.append(value);
    }

    public void addRecord(String sourceId, String propertyKey, char value) {
        addSourceId(sourceId, propertyKey);
        sb.append(value);
    }

    public void addRecord(String sourceId, String propertyKey, CharSequence value) {
        addSourceId(sourceId, propertyKey);
        sb.append(value);
    }

    public void addRecord(String sourceId, String propertyKey, Object value) {
        addSourceId(sourceId, propertyKey);
        sb.append(value == null ? "NULL" : value);
    }

    public void addRecord(String sourceId, String propertyKey, boolean value) {
        addSourceId(sourceId, propertyKey);
        sb.append(value);
    }

    public void addTrace(String sourceId) {
        if (this.sourceId != null) {
            sb.append("}");
        }
        firstProp = true;
        this.sourceId = null;
        addSourceId(sourceId, null);
    }

    public void printEventToString(boolean printEventToString) {
        this.printEventToString = printEventToString;
    }

    protected void addSourceId(String sourceId, String propertyKey) {
        if (loggingEnabled()) {
            if (this.sourceId == null) {
                sb.append("\n        - ").append(sourceId).append(": {");
                this.sourceId = sourceId;
            } else if (!this.sourceId.equals(sourceId)) {
                sb.append("}\n        - ").append(sourceId).append(": {");
                this.sourceId = sourceId;
                firstProp = true;
            }
            if (!firstProp) {
                sb.append(",");
            }
            if (propertyKey != null) {
                firstProp = false;
                sb.append(" ").append(propertyKey).append(": ");
            }
        }
    }

    /**
     * The time event processing began, for the {@code logTime} field.
     *
     * <p><b>This is {@link Clock#getProcessTime()}, not a fresh wall-clock reading, and that is a
     * correctness fix as much as a performance one.</b> {@code logTime} is defined as "the time the
     * log record is created i.e. when the event processing began". {@link Clock#eventReceived} has
     * <em>already</em> taken exactly that reading for this event and cached it. Taking a second,
     * later reading here reported a {@code logTime} some nanoseconds after processing actually began,
     * and paid for a {@code System.currentTimeMillis()} call on every record to be less accurate.
     *
     * <p>Measured on a 30-node graph, 5 event types, minimal audit profile: <b>13.7 ns/event</b> on
     * the text record under JIT, ~0 on the same record under native AOT, and 10.4 ns (JIT) /
     * 12.5 ns (native) on a binary record subclass. A wall-clock read is 12.3 ns in isolation on that
     * machine, so the marginal cost is roughly half the isolated cost.
     *
     * <p><b>Ordering requirement.</b> This is correct only while {@link Clock#eventReceived} runs
     * before {@code EventLogManager.eventReceived} for the same event. The generated processor emits
     * {@code clock.eventReceived(...)} first today, but that follows auditor registration order and is
     * not yet enforced by the generator — see the binary-audit-encoding spec, which makes it normative.
     *
     * <p>{@code endTime} deliberately keeps a live reading: {@code endTime - logTime} is the
     * processing duration, and a cached value would report it as zero.
     *
     * @return the wall-clock time at which processing of the current event began
     */
    protected long logTime() {
        return clock.getProcessTime();
    }

    public void clear() {
        firstProp = true;
        sourceId = null;
        sb.setLength(0);
    }

    public CharSequence asCharSequence() {
        return sb;
    }

    public void triggerEvent(Event event) {
        if (loggingEnabled()) {
            Class<? extends Event> aClass = event.getClass();
            sb.append("eventLogRecord: ");
            sb.append("\n    eventTime: ");
            timeFormatter.accept(sb, clock.getEventTime());

            sb.append("\n    logTime: ");
            timeFormatter.accept(sb, logTime());

            sb.append("\n    groupingId: ").append(groupingId);
            sb.append("\n    event: ").append(aClass.getSimpleName());
            if (printEventToString) {
                sb.append("\n    eventToString: ").append(event.toString());
            }
            if (printThreadName) {
                sb.append("\n    thread: ").append(Thread.currentThread().getName());
            }
            if (event.filterString() != null && !event.filterString().isEmpty()) {
                sb.append("\n    eventFilter: ").append(event.filterString());
            }
            sb.append("\n    nodeLogs: ");
        }
    }

    public void triggerObject(Object event) {
        if (loggingEnabled()) {
            if (event instanceof Event) {
                triggerEvent((Event) event);
            } else {
                Class<?> aClass = event.getClass();
                sb.append("eventLogRecord: ");
                sb.append("\n    eventTime: ");
                timeFormatter.accept(sb, clock.getEventTime());

                sb.append("\n    logTime: ");
                timeFormatter.accept(sb, logTime());

                sb.append("\n    groupingId: ").append(groupingId);

                sb.append("\n    event: ").append(aClass.getSimpleName());
                if (printEventToString) {
                    sb.append("\n    eventToString: ").append(event.toString());
                }
                if (printThreadName) {
                    sb.append("\n    thread: ").append(Thread.currentThread().getName());
                }
                sb.append("\n    nodeLogs: ");
            }
        }
    }

    /**
     * complete record processing, the return value indicates if any log values
     * were written.
     *
     * @return flag to indicate properties were logged
     */
    public boolean terminateRecord() {
        boolean logged = !firstProp;
        if (loggingEnabled()) {
            if (this.sourceId != null) {
                sb.append("}");
            }
            if (recordEndTime) {
                sb.append("\n    endTime: ");
                timeFormatter.accept(sb, clock.getWallClockTime());
            }
        }
        firstProp = true;
        sourceId = null;
        return logged;
    }

    @Override
    public String toString() {
        return asCharSequence().toString();
    }

    protected boolean loggingEnabled() {
        return logLevel != EventLogControlEvent.LogLevel.NONE;
    }
}
