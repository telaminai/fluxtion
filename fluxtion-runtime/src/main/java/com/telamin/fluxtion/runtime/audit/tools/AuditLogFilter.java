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
    private boolean anyEventMatched;
    private boolean anyNodeMatched;
    private boolean anyKeyMatched;

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
        if (matches(eventGlob, name)) { eventIds.set(id); anyEventMatched = true; }
        if (matches(nodeGlob, name)) { nodeIds.set(id); anyNodeMatched = true; }
        if (matches(keyGlob, name)) { keyIds.set(id); anyKeyMatched = true; }
    }

    @Override
    public boolean onRecord(int eventTypeId, String eventType,
                            long eventTime, long logTime, long endTime, int entryCount) {
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
        matchedRecords++;
        sink.record(eventType, eventTime, logTime, endTime);
        recordOpen = true;
        return true;
    }

    @Override
    public void onEntry(int nodeId, String node, int keyId, String key, int tag, long rawBits) {
        if (!recordOpen) {
            return;
        }
        if (nodeGlob != null && !nodeIds.get(nodeId)) {
            return;
        }
        if (keyGlob != null && !keyIds.get(keyId)) {
            return;
        }
        matchedEntries++;
        sink.entry(node, key, BinaryRecordDecoder.renderValue(tag, rawBits));
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
    public String unmatchablePattern() {
        if (eventGlob != null && !anyEventMatched) { return "--event " + eventGlob; }
        if (nodeGlob != null && !anyNodeMatched) { return "--node " + nodeGlob; }
        if (keyGlob != null && !anyKeyMatched) { return "--key " + keyGlob; }
        return null;
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
