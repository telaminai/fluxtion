/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit.tools;

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
    private final long from;
    private final long to;
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
    private final BitSet eventIdsSeen = new BitSet();
    private final BitSet nodeIdsSeen = new BitSet();
    private final BitSet keyIdsSeen = new BitSet();

    /** Held until an entry survives the node/key filters — see {@link #onEntry}. */
    private String pendingEventType;
    private long pendingEventTime;
    private long pendingLogTime;
    private long pendingEndTime;
    private boolean recordPending;

    private long matchedRecords;
    private long matchedEntries;
    private boolean recordOpen;

    public AuditLogFilter(String eventGlob, String nodeGlob, String keyGlob,
                          long from, long to, long limit, Sink sink) {
        this.eventGlob = eventGlob;
        this.nodeGlob = nodeGlob;
        this.keyGlob = keyGlob;
        this.from = from;
        this.to = to;
        this.limit = limit;
        this.sink = sink;
    }

    @Override
    public void onDictionaryEntry(int id, String name) {
        while (namesById.size() <= id) {
            namesById.add(null);
        }
        namesById.set(id, name);
        if (matches(eventGlob, name)) { eventIds.set(id); }
        if (matches(nodeGlob, name)) { nodeIds.set(id); }
        if (matches(keyGlob, name)) { keyIds.set(id); }
    }

    @Override
    public boolean onRecord(int eventTypeId, String eventType,
                            long eventTime, long logTime, long endTime, int entryCount) {
        // ROLE DISCOVERY FIRST, before any filter can decline the record. It answers "does this name
        // exist in this role ANYWHERE in the file", which is what the unmatchable diagnostic claims -
        // and recording it after the time, limit and event filters made the answer a property of the
        // query it was meant to explain. A node present only under another event was then reported as
        // absent from the file, which is false: the name exists, the combination does not match.
        eventIdsSeen.set(eventTypeId);
        recordOpen = false;
        if (matchedRecords >= limit) {
            return false;
        }
        if (logTime < from || logTime > to) {
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
        nodeIdsSeen.set(nodeId);
        keyIdsSeen.set(keyId);
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
    /**
     * The pattern that matched no name in its own role <b>within the records this query reached</b>,
     * or null.
     *
     * <p>Asks whether any id used as an event type / node / key matches the glob, rather than whether
     * any dictionary name anywhere does. The dictionary is one untyped id space, so the weaker question
     * answered "yes" for an {@code --event} pattern that only ever matched a node name.
     *
     * <p><b>The scope is the selection, not the file, and the caller must say so.</b> Entries are only
     * offered for records the event and time filters admitted — that early skip is the cheap path those
     * filters exist for — so a node appearing only under another event is not observed here. Claiming
     * whole-file absence from this would be false: the name exists, the combination does not match.
     * Answering the stronger question would mean reading every record of every file, which is a
     * different feature with a different cost.
     */
    public String unmatchableWithinSelection() {
        if (eventGlob != null && noneSeenMatching(eventIds, eventIdsSeen)) { return "--event " + eventGlob; }
        if (nodeGlob != null && noneSeenMatching(nodeIds, nodeIdsSeen)) { return "--node " + nodeGlob; }
        if (keyGlob != null && noneSeenMatching(keyIds, keyIdsSeen)) { return "--key " + keyGlob; }
        return null;
    }

    private static boolean noneSeenMatching(BitSet matchedIds, BitSet seenInRole) {
        return !matchedIds.intersects(seenInRole);
    }

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
