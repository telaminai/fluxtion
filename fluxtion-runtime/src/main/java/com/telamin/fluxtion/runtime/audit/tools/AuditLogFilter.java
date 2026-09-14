/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit.tools;

import com.telamin.fluxtion.runtime.audit.BinaryLogFile;
import com.telamin.fluxtion.runtime.audit.BinaryLogReader;
import com.telamin.fluxtion.runtime.audit.BinaryRecordDecoder;

import java.util.BitSet;

/**
 * Filters a binary audit log by time range, event type, node and property key.
 *
 * <h2>Patterns are resolved to ids once, never matched per entry</h2>
 * A glob is evaluated against each <b>dictionary entry</b> as it arrives — a few dozen times for a
 * whole file — and the matching ids go into a {@link BitSet}. Every entry thereafter is filtered by an
 * integer set test. Matching text per entry would cost more than reading the file: the round that
 * produced this format measured name <em>resolution</em> alone at a third of the record cost, and that
 * was a lookup, not a pattern match.
 *
 * <h2>A pattern that matches nothing short-circuits</h2>
 * If a glob matches no dictionary entry, nothing can match it, and the filter says so rather than
 * scanning every record to discover it.
 */
public final class AuditLogFilter implements BinaryLogReader.Visitor {

    /** Where accepted entries go. Swap it for a different output without touching the filter. */
    public interface Sink {
        void record(String eventType, long eventTime, long logTime, long endTime);

        void entry(String node, String key, String value);

        default void close() {
        }
    }

    private final String eventGlob;
    private final String nodeGlob;
    private final String keyGlob;
    /** The bounds AS GIVEN, in epoch milliseconds - the unit the CLI documents; null when not given. */
    private final Long fromMillis;
    private final Long toMillis;
    /** The bounds in the FILE's unit, set at the header. Until then, everything passes. */
    private long from = Long.MIN_VALUE;
    private long to = Long.MAX_VALUE;
    /** A bound that no representable timestamp can satisfy - the range is empty, not clamped. */
    private boolean empty;
    private int timeUnit = BinaryLogFile.TIME_UNIT_UNSPECIFIED;
    private final long limit;
    private final Sink sink;

    private final BitSet eventIds = new BitSet();
    private final BitSet nodeIds = new BitSet();
    private final BitSet keyIds = new BitSet();

    /**
     * Names by id, so a value written as a dictionary id can be rendered as its text.
     *
     * <p>String and Object audit values are stored as ids in the value slot. Without this the shipped
     * CLI printed them as {@code #tag5:4} — the raw tag and id — because the renderer had no dictionary.
     */
    private final java.util.List<String> namesById = new java.util.ArrayList<>();

    /**
     * Which ids were actually USED in each role.
     *
     * <p>The dictionary is untyped: one id space for event types, node names, keys and string values.
     * Matching a glob against every name therefore said "--event matched" when the pattern only ever
     * matched a NODE name, so {@link #unmatchableWithinSelection()} stayed silent and the CLI reported an empty
     * result instead of "nothing in this log is called that". Role is only knowable where an id is
     * consumed, so it is recorded there.
     */
    private boolean eventMatchedWhenSeen;
    private boolean nodeMatchedWhenSeen;
    private boolean keyMatchedWhenSeen;

    /** Held until an entry survives the node/key filters — see {@link #onEntry}. */
    private String pendingEventType;
    private long pendingEventTime;
    private long pendingLogTime;
    private long pendingEndTime;
    private boolean recordPending;

    private long matchedRecords;
    private long matchedEntries;
    private boolean recordOpen;

    /**
     * The sentinel form: {@code Long.MIN_VALUE} for {@code from} and {@code Long.MAX_VALUE} for
     * {@code to} mean "not given". Prefer the boxed constructor, where absence is {@code null} and
     * an explicitly supplied extreme is a real bound.
     */
    public AuditLogFilter(String eventGlob, String nodeGlob, String keyGlob,
                          long from, long to, long limit, Sink sink) {
        this(eventGlob, nodeGlob, keyGlob,
                from == Long.MIN_VALUE ? null : Long.valueOf(from),
                to == Long.MAX_VALUE ? null : Long.valueOf(to), limit, sink);
    }

    /**
     * @param fromMillis inclusive lower bound on {@code logTime}, in epoch milliseconds; null for none
     * @param toMillis   inclusive upper bound, likewise
     */
    public AuditLogFilter(String eventGlob, String nodeGlob, String keyGlob,
                          Long fromMillis, Long toMillis, long limit, Sink sink) {
        this.eventGlob = eventGlob;
        this.nodeGlob = nodeGlob;
        this.keyGlob = keyGlob;
        this.fromMillis = fromMillis;
        this.toMillis = toMillis;
        this.limit = limit;
        this.sink = sink;
    }

    /** True when the caller asked for a time range at all. Presence, not a magic value. */
    private boolean boundsGiven() {
        return fromMillis != null || toMillis != null;
    }

    /**
     * The bounds are documented as milliseconds and the file's timestamps are in whatever unit its
     * header states, so the comparison is only meaningful once the header has been read. A review ran
     * {@code --from 1000000000000 --to 2000000000000} - a range holding 2026 in milliseconds - over a
     * file declaring nanoseconds and got "records matched: 0", exit 0, no warning: the millisecond
     * bounds were compared to nanosecond readings as they stood.
     *
     * <ul>
     *   <li>milliseconds: the bounds apply as given</li>
     *   <li>nanoseconds: the bounds are scaled by a million. A bound outside the representable
     *       nanosecond domain keeps its INEQUALITY rather than being clamped: a lower bound above
     *       every representable instant admits nothing, and an upper bound below every representable
     *       instant admits nothing, where clamping to {@code Long.MAX_VALUE} had admitted a record
     *       stamped exactly there. Bounds are inclusive.</li>
     *   <li>unspecified or undefined: a time query cannot mean anything, so it is refused. Raw
     *       inspection without bounds still works, and {@code --stats} still labels the code.</li>
     * </ul>
     *
     * @throws IllegalArgumentException when bounds were given and the unit cannot honour them
     */
    @Override
    public void onHeader(int formatVersion, int unit) {
        this.timeUnit = unit;
        if (!boundsGiven()) {
            return;
        }
        switch (unit) {
            case BinaryLogFile.TIME_UNIT_EPOCH_MILLIS:
                from = fromMillis == null ? Long.MIN_VALUE : fromMillis;
                to = toMillis == null ? Long.MAX_VALUE : toMillis;
                return;
            case BinaryLogFile.TIME_UNIT_EPOCH_NANOS:
                if (fromMillis != null) {
                    if (fromMillis > Long.MAX_VALUE / 1_000_000L) empty = true;          // after every instant
                    else if (fromMillis < Long.MIN_VALUE / 1_000_000L) from = Long.MIN_VALUE; // before every instant
                    else from = fromMillis * 1_000_000L;
                }
                if (toMillis != null) {
                    if (toMillis < Long.MIN_VALUE / 1_000_000L) empty = true;             // before every instant
                    else if (toMillis > Long.MAX_VALUE / 1_000_000L) to = Long.MAX_VALUE;   // after every instant
                    else to = toMillis * 1_000_000L;
                }
                return;
            case BinaryLogFile.TIME_UNIT_UNSPECIFIED:
                throw new IllegalArgumentException("--from/--to are milliseconds, and this file's header "
                        + "does not state its unit (code 0: written before the unit field existed). A "
                        + "time query cannot be honoured. Declare the unit into a copy with "
                        + "--declare-unit millis|nanos --out <copy>, or query without bounds.");
            default:
                throw new IllegalArgumentException("--from/--to are milliseconds, and this file's header "
                        + "carries time unit code " + unit + ", which the format does not define. A time "
                        + "query cannot be honoured; query without bounds to inspect the raw records.");
        }
    }

    /** The header's unit code, as read; {@code TIME_UNIT_UNSPECIFIED} before the header. */
    public int timeUnit() {
        return timeUnit;
    }

    @Override
    public void onDictionaryEntry(int id, String name) {
        while (namesById.size() <= id) {
            namesById.add(null);
        }
        namesById.set(id, name);
        // SET OR CLEAR. A redefinition (format §4: the latest definition names what follows) must
        // not leave an id matching the pattern its OLD name matched: the bits are the answer to
        // "does this id's current name match", not a memory of every name it ever had.
        eventIds.set(id, matches(eventGlob, name));
        nodeIds.set(id, matches(nodeGlob, name));
        keyIds.set(id, matches(keyGlob, name));
    }

    @Override
    public boolean onRecord(int eventTypeId, String eventType,
                            long eventTime, long logTime, long endTime, int entryCount) {
        // The EVENT role is observed for every record the reader offers, before any filter declines
        // it. NODE and KEY roles cannot be: entries arrive only for records this method admitted, and
        // that early skip is the cheap path the event and time filters exist for. So the three roles
        // have different scopes, and unmatchableWithinSelection() states the narrower one for all of
        // them rather than claiming the wider one for any.
        // WHEN it was seen, against its name AT THAT MOMENT. A redefinition (format §4) changes what an
        // id means for the frames after it; a set of ids joined with the final names said "nothing
        // matched" about a selection that had matched, and would say the opposite for a name renamed
        // to match after its last use.
        if (eventGlob != null && eventIds.get(eventTypeId)) { eventMatchedWhenSeen = true; }
        recordOpen = false;
        if (matchedRecords >= limit) {
            return false;
        }
        if (empty || logTime < from || logTime > to) {
            return false;
        }
        if (eventGlob != null && !eventIds.get(eventTypeId)) {
            return false;
        }
        // WITH AN ENTRY FILTER, A RECORD IS NOT MATCHED UNTIL AN ENTRY MATCHES. Counting it here spent
        // --limit on records that contained nothing the caller asked for, so `--node x --limit 1` could
        // report empty when the second record held the only x. The header is therefore held and emitted
        // by the first surviving entry.
        if (nodeGlob != null || keyGlob != null) {
            pendingEventType = eventType;
            pendingEventTime = eventTime;
            pendingLogTime = logTime;
            pendingEndTime = endTime;
            recordPending = true;
            recordOpen = true;
            return true;
        }
        matchedRecords++;
        sink.record(eventType, eventTime, logTime, endTime);
        recordOpen = true;
        return true;
    }

    @Override
    public void onEntry(int nodeId, String node, int keyId, String key, int tag, long rawBits) {
        // As in onRecord: observe the role before any filter declines the entry.
        if (nodeGlob != null && nodeIds.get(nodeId)) { nodeMatchedWhenSeen = true; }
        if (keyGlob != null && keyIds.get(keyId)) { keyMatchedWhenSeen = true; }
        if (!recordOpen) {
            return;
        }
        if (nodeGlob != null && !nodeIds.get(nodeId)) {
            return;
        }
        if (keyGlob != null && !keyIds.get(keyId)) {
            return;
        }
        if (recordPending) {
            recordPending = false;
            matchedRecords++;
            sink.record(pendingEventType, pendingEventTime, pendingLogTime, pendingEndTime);
        }
        matchedEntries++;
        sink.entry(node, key, BinaryRecordDecoder.renderValue(tag, rawBits, this::nameById));
    }

    /** Resolves a dictionary id for the value renderer; {@code null} when the id has no entry. */
    private String nameById(int id) {
        return id >= 0 && id < namesById.size() ? namesById.get(id) : null;
    }

    /**
     * The pattern that matched no name in its own role <b>within the records this query reached</b>,
     * or null.
     *
     * <p>Asks whether any id used as an event type / node / key matched the glob <em>at the moment it
     * was used</em>, rather than whether any dictionary name anywhere does. The dictionary is one
     * untyped id space, so the weaker question answered "yes" for an {@code --event} pattern that only
     * ever matched a node name. And it is historical query evidence, not current match state: after a
     * redefinition (format §4) an id's final name says nothing about what it was called when a
     * selected record used it. Joining the ids ever seen with their final names called a non-empty
     * result empty, and would call a name renamed to match after its last use a match (review, round 8).
     *
     * <p><b>The three roles have different scopes, and the caller must claim the narrower one.</b>
     * The EVENT role is observed for every record the reader offers, before any filter — so an
     * {@code --event} pattern reported here genuinely matched no event type in the file. The NODE and
     * KEY roles are observed only in records the event and time filters admitted, because entries are
     * offered only for those; that early skip is the cheap path the filters exist for. So a
     * {@code --node} or {@code --key} pattern reported here matched nothing <em>within the selection</em>,
     * and a node appearing only under another event is not observed. Claiming whole-file absence for
     * those two would be false: the name exists, the combination does not match.
     */
    public String unmatchableWithinSelection() {
        if (eventGlob != null && !eventMatchedWhenSeen) { return "--event " + eventGlob; }
        if (nodeGlob != null && !nodeMatchedWhenSeen) { return "--node " + nodeGlob; }
        if (keyGlob != null && !keyMatchedWhenSeen) { return "--key " + keyGlob; }
        return null;
    }

    public long matchedRecords() {
        return matchedRecords;
    }

    public long matchedEntries() {
        return matchedEntries;
    }

    /**
     * A glob that matched no name in the whole file. Nothing can match it, so a caller can say so
     * rather than reporting an empty result as though the log simply had nothing of interest.
     */

    /** {@code *} and {@code ?} only — enough for names, and no regex compilation per file. */
    static boolean matches(String glob, String name) {
        if (glob == null || name == null) {
            return glob == null;
        }
        return matches(glob, 0, name, 0);
    }

    private static boolean matches(String g, int gi, String n, int ni) {
        while (gi < g.length()) {
            char c = g.charAt(gi);
            if (c == '*') {
                for (int skip = ni; skip <= n.length(); skip++) {
                    if (matches(g, gi + 1, n, skip)) {
                        return true;
                    }
                }
                return false;
            }
            if (ni >= n.length()) {
                return false;
            }
            if (c != '?' && c != n.charAt(ni)) {
                return false;
            }
            gi++;
            ni++;
        }
        return ni == n.length();
    }
}
