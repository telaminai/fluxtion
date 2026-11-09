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
 * <h2>Text encoding marker</h2>
 * String-valued fields are written through {@link YamlScalar}: plain when that reads back as the same
 * string, YAML double-quoted otherwise. nodeLogs values sit in a flow map and use the flow rule
 * ({@link YamlScalar#append}); the header's groupingId, event, eventToString, thread and eventFilter
 * run to the end of their line and use the narrower block rule ({@link YamlScalar#appendBlock}), so a
 * typical {@code toString()} such as {@code Trade[id=1, px=2.0]} stays plain.
 * Typed-looking strings ({@code 5000}, {@code true}, {@code null}, {@code 0x1F}, a date) are written
 * bare and read with YAML/Analyser typing, as before; only structurally unsafe text is quoted. A record holding <b>at least one</b> double-quoted scalar carries one
 * extra line, always the first line of its {@code eventLogRecord} mapping:
 *
 * <pre>
 * eventLogRecord:
 *     textEncoding: QUOTED_SCALARS
 *     eventTime: 1578236915413
 *     ...
 *     nodeLogs:
 *         - gate: { reason: "not active, balance due 30.0"}
 * </pre>
 *
 * It tells a reader whose legacy grammar treats quote marks as literal characters that this record's
 * quoted scalars must be decoded. A reader honours it only in that first position, so a logged value
 * (always quoted if it contains a line break, whose escape keeps it on one line) can never forge it. A record
 * with no quoted value is byte-for-byte what earlier releases wrote: no marker.
 *
 * <p><b>Placement cost.</b> The header is written by {@code triggerEvent}/{@code triggerObject}
 * before any value is known, so the marker is inserted at the header's recorded offset by
 * {@link #terminateRecord()} — one {@code StringBuilder.insert} of a constant, done only on a record
 * that needs it. The common path pays one boolean OR per string value and allocates nothing extra.
 * A subclass writing its own header should call {@link #markRecordStart()} before the key; one that
 * does not is found by a backward search for the key at a line start, again only on a marked record.
 *
 * <p><b>Escapes.</b> Inside a double-quoted scalar exactly these are emitted: {@code \\},
 * {@code \x22} (the double quote; never {@code \"}), {@code \n}, {@code \r}, {@code \t}, {@code \0},
 * and {@code \}{@code uXXXX} (BMP, lowercase hex) for any other non-printable character. A valid
 * surrogate pair is written as-is; an unpaired surrogate is written as {@code \}{@code ufffd}. See
 * {@link YamlScalar}.
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
    protected boolean terminated;
    protected boolean terminatedWithProperties;
    /** The header line every text record starts with; the marker is inserted directly after it. */
    static final String RECORD_KEY = "eventLogRecord: ";
    /** The marker line, written only on a record holding at least one double-quoted scalar. */
    public static final String TEXT_ENCODING_MARKER = "\n    textEncoding: QUOTED_SCALARS";
    /** True once the current record has written a value in double-quoted form. */
    protected boolean quotedScalars;
    /** Offset in {@link #sb} of the current record's {@code eventLogRecord:} key; -1 when no header. */
    protected int recordStart = -1;
    /**
     * Whether to take a second clock reading for {@code endTime}. <b>On by default.</b>
     *
     * <p>{@code endTime} exists so {@code endTime - logTime} gives the processing duration, and it has
     * to be a live reading to do that — a cached one reports every event as taking zero time. But that
     * costs a clock read on every event, and on any event faster than the clock's resolution the answer
     * is zero anyway. An audited event path reads a clock twice per event; this is the second one, and
     * most deployments do not need it.
     *
     * <p><b>Default TRUE, because 1.0.13 and every release before it emitted {@code endTime} on every
     * record.</b> This flag is new; defaulting it off silently removed a field from the default text
     * output, which is a changed output contract rather than the additive change this release claims.
     * A consumer parsing the record for {@code endTime} would have found it gone. The saving is real
     * and is still available - set this false - but it is opted into
     * rather than imposed.
     *
     * <p>Turn it off when you do not consume the duration. Leave it on, and pair it with a clock that
     * can resolve the interval, when you do:
     * {@link com.telamin.fluxtion.runtime.time.ClockStrategy#nanoEpochClock()}.
     */
    protected boolean recordEndTime = true;
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
            resetTermination();
            resetRecordState();
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
        resetTermination();
        resetRecordState();
    }

    /**
     * Carries the in-progress record's marker state from {@code previous}, whose buffer this record
     * has just taken with {@link #replaceBuffer}, so a quoted value written before the swap still
     * marks the record.
     */
    void adoptRecordState(LogRecord previous) {
        this.quotedScalars = previous.quotedScalars;
        this.recordStart = previous.recordStart;
        // The open node entry too: without these the next value opened a second "- node: {" with the
        // first never closed, or wrote a key-less entry.
        this.sourceId = previous.sourceId;
        this.firstProp = previous.firstProp;
    }

    /**
     * Records that the current record's {@code eventLogRecord: } key starts at the buffer's current
     * end. A subclass that writes its own header calls this immediately before appending the key, so
     * the {@code textEncoding} marker is inserted directly after it. A subclass that does not is
     * still marked: {@link #terminateRecord()} then searches back for the last key at a line start.
     */
    protected void markRecordStart() {
        recordStart = sb.length();
    }

    private void resetRecordState() {
        quotedScalars = false;
        recordStart = -1;
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

    public void addRecord(int sourceRef, int keyRef, char value) {
        throw new UnsupportedOperationException("record returned ids from internName but did not "
                + "override addRecord(int, int, char)");
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
        quotedScalars |= YamlScalar.append(sb, value);
    }

    /**
     * A string value is written as runtime 1.1.3 wrote it - bare - unless that would break the record's
     * structure, and then YAML double-quoted - see {@link YamlScalar}. Written bare,
     * {@code "not active, balance due 30.0"} was cut at the comma by every YAML reader. Only structural
     * breakage is prevented: typed-looking text such as {@code 123}, {@code true} or {@code 12:30:00}
     * stays bare and reads with YAML typing, as before. For a value that must read back as a String,
     * use {@link #addTextRecord} ({@link EventLogger#infoText} and friends).
     */
    public void addRecord(String sourceId, String propertyKey, CharSequence value) {
        addSourceId(sourceId, propertyKey);
        quotedScalars |= YamlScalar.append(sb, value);
    }

    /**
     * A value the producer declared to be <b>text</b>: written so every reader types it as a String,
     * even when it looks like a number, boolean or null ({@code 472.1}, {@code 5000}, {@code true},
     * the empty string) - see {@link YamlScalar#appendText}. A quoted value marks the record as usual.
     * {@link #addRecord(String, String, CharSequence)} keeps the 1.1.3 rule: typed-looking text bare.
     *
     * <p>A subclass that overrides {@link #addRecord(String, String, CharSequence)} to write somewhere
     * other than this record's YAML buffer must override this too, as {@code BinaryLogRecord} does.
     */
    public void addTextRecord(String sourceId, String propertyKey, CharSequence value) {
        addSourceId(sourceId, propertyKey);
        quotedScalars |= YamlScalar.appendText(sb, value);
    }

    public void addRecord(String sourceId, String propertyKey, Object value) {
        addSourceId(sourceId, propertyKey);
        // "null", not "NULL". The analyser's format spec reads the lowercase literal as a null value and
        // anything else as a String, so the uppercase spelling reached it as the string "NULL" - and the
        // CharSequence overload already wrote lowercase, so the two null paths disagreed with each other
        // as well as with the reader. Existing logs carrying "NULL" are unchanged and still parse as the
        // String they always parsed as; only new output moves.
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Number || value instanceof Boolean) {
            // logged as a number or boolean: stays an unquoted YAML number/boolean
            sb.append(value);
        } else if (value instanceof Character) {
            quotedScalars |= YamlScalar.append(sb, (char) (Character) value);
        } else if (value instanceof CharSequence) {
            quotedScalars |= YamlScalar.append(sb, (CharSequence) value);
        } else {
            quotedScalars |= YamlScalar.append(sb, value.toString());
        }
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
        resetTermination();
        firstProp = true;
        sourceId = null;
        sb.setLength(0);
        resetRecordState();
    }

    public CharSequence asCharSequence() {
        return sb;
    }

    public void triggerEvent(Event event) {
        resetTermination();
        if (loggingEnabled()) {
            Class<? extends Event> aClass = event.getClass();
            markRecordStart();
            sb.append(RECORD_KEY);
            sb.append("\n    eventTime: ");
            timeFormatter.accept(sb, clock.getEventTime());

            sb.append("\n    logTime: ");
            timeFormatter.accept(sb, logTime());

            appendHeaderFields(event, aClass);
            if (event.filterString() != null && !event.filterString().isEmpty()) {
                sb.append("\n    eventFilter: ");
                quotedScalars |= YamlScalar.appendBlock(sb, event.filterString());
            }
            sb.append("\n    nodeLogs: ");
        }
    }

    public void triggerObject(Object event) {
        resetTermination();
        if (loggingEnabled()) {
            if (event instanceof Event) {
                triggerEvent((Event) event);
            } else {
                Class<?> aClass = event.getClass();
                markRecordStart();
                sb.append(RECORD_KEY);
                sb.append("\n    eventTime: ");
                timeFormatter.accept(sb, clock.getEventTime());

                sb.append("\n    logTime: ");
                timeFormatter.accept(sb, logTime());

                appendHeaderFields(event, aClass);
                sb.append("\n    nodeLogs: ");
            }
        }
    }

    /**
     * groupingId, event, eventToString and thread, each written through
     * {@link YamlScalar#appendBlock}: a header field runs to the end of its line, so only what would
     * break or re-type it there is quoted, and a typical {@code toString()} stays plain.
     */
    private void appendHeaderFields(Object event, Class<?> aClass) {
        sb.append("\n    groupingId: ");
        quotedScalars |= YamlScalar.appendBlock(sb, groupingId);
        sb.append("\n    event: ");
        String simpleName = aClass.getSimpleName();
        if (!simpleName.isEmpty()) {
            // an anonymous class has no simple name: left bare, as every release wrote it
            quotedScalars |= YamlScalar.appendBlock(sb, simpleName);
        }
        if (printEventToString) {
            sb.append("\n    eventToString: ");
            quotedScalars |= YamlScalar.appendBlock(sb, event.toString());
        }
        if (printThreadName) {
            sb.append("\n    thread: ");
            quotedScalars |= YamlScalar.appendBlock(sb, Thread.currentThread().getName());
        }
    }

    /**
     * complete record processing, the return value indicates if any log values
     * were written. Retrying a completed close leaves the record unchanged and returns
     * the original result, so a failed listener can retry publication without losing data.
     * A new event, {@link #clear()} or {@link #replaceBuffer(CharSequence)} starts a new close.
     *
     * @return flag to indicate properties were logged
     */
    public boolean terminateRecord() {
        if (terminated) {
            return terminatedWithProperties;
        }
        boolean logged = !firstProp;
        if (loggingEnabled()) {
            if (this.sourceId != null) {
                sb.append("}");
            }
            if (recordEndTime) {
                sb.append("\n    endTime: ");
                timeFormatter.accept(sb, clock.getWallClockTime());
            }
            if (quotedScalars) {
                // Rare path only: the header was written before any value was known, so the marker
                // goes in at its offset now. Records with no quoted value never reach here.
                int start = recordStart >= 0 ? recordStart : lastRecordKeyAtLineStart();
                if (start >= 0) {
                    sb.insert(start + RECORD_KEY.length(), TEXT_ENCODING_MARKER);
                }
            }
        }
        firstProp = true;
        sourceId = null;
        resetRecordState();
        terminatedWithProperties = logged;
        terminated = true;
        return logged;
    }

    /**
     * The offset of the last {@code eventLogRecord: } key that starts a line, or -1: the fallback for a
     * subclass header that did not call {@link #markRecordStart()}. A logged value can contain the key
     * text, but never at a line start - a line break inside a value is always escaped.
     */
    private int lastRecordKeyAtLineStart() {
        int i = sb.lastIndexOf(RECORD_KEY);
        while (i > 0 && sb.charAt(i - 1) != '\n') {
            i = sb.lastIndexOf(RECORD_KEY, i - 1);
        }
        return i;
    }

    /** Whether anything has been written since the record was last cleared. */
    boolean hasContent() {
        return sb.length() > 0;
    }

    /** Reset by record implementations when starting or replacing their content. */
    protected final void resetTermination() {
        terminated = false;
        terminatedWithProperties = false;
    }

    @Override
    public String toString() {
        return asCharSequence().toString();
    }

    protected boolean loggingEnabled() {
        return logLevel != EventLogControlEvent.LogLevel.NONE;
    }
}
