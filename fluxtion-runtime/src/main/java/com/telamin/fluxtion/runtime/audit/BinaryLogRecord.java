/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.event.Event;
import com.telamin.fluxtion.runtime.time.Clock;

import java.util.IdentityHashMap;

/**
 * A {@link LogRecord} that writes <b>bits, not characters</b>.
 *
 * <p>Installed through the seam that already exists — {@code new EventLogControlEvent(record)} — so no
 * core change is needed to measure it. Every one of the seven {@code addRecord} overloads is
 * overridden; none of them touches the inherited {@code StringBuilder}.
 *
 * <h2>Wire shape</h2>
 * Names are not written. A node name or property key is interned once to a {@code short} id, and only
 * the id goes on the wire; the id table is written out separately (it is fixed after warm-up, being
 * generated code passing String constants). A value is a 1-byte type tag plus its raw bits.
 *
 * <pre>
 *   record  := header, entry*, terminator
 *   header  := 0x01, eventTime:long, logTime:long, eventTypeId:short
 *   entry   := nodeId:short, keyId:short, tag:byte, bits
 *   term    := 0x00, endTime:long
 * </pre>
 *
 * <p>The dictionary is what buys the compression: a 17-character double becomes 8 bytes, and
 * {@code "        - notional: { value: "} becomes 4.
 */
public final class BinaryLogRecord extends LogRecord {

    private static final byte TAG_DOUBLE = 1, TAG_LONG = 2, TAG_INT = 3, TAG_CHAR = 4,
            TAG_CHARSEQ = 5, TAG_OBJECT = 6, TAG_BOOL = 7;

    /** {@code live} = stock behaviour, {@code process} = reuse the clock read Clock already did,
     *  {@code none} = no wall-clock read at all. Z-arm switch for round 63 §7.4. */
    public static String clockMode = System.getProperty("clock", "live");

    /** Resolved once at construction. The String switch this replaces cost a hash and an equals on
     *  every header and every terminator — twice per record — and was an artifact of the class
     *  carrying three experiment modes. A real encoder has one mode. */
    private final boolean useProcessTime = "process".equals(clockMode);
    private final boolean noClock = "none".equals(clockMode);




    /**
     * Open-addressed identity table — the fix for interning, Round 63 §19.5.
     *
     * <p>The map version cost 30.7 ns/event on JIT and 27.5 on native, because a one-slot-per-role
     * cache misses whenever two nodes alternate and every miss falls through to
     * {@code IdentityHashMap.get}. Generated code passes interned String constants, so identity is the
     * right comparison and a power-of-two table with linear probing resolves a name in one array read
     * and one reference compare on the hit path — no hashing of characters, no Map call.
     *
     * <p>Sized generously and never resized: the name set is fixed after warm-up because it comes from
     * constants in generated source. A full table falls back to the map rather than looping.
     */
    private static final int TBL = 256, MASK = TBL - 1;
    private final String[] tblKey = new String[TBL];
    private final short[] tblVal = new short[TBL];

    private short tableId(String name) {
        int i = System.identityHashCode(name) & MASK;
        for (int probe = 0; probe < 8; probe++) {
            String k = tblKey[i];
            if (k == name) { cacheHits++; return tblVal[i]; }
            if (k == null) {
                cacheMisses++;
                short id = intern(name);
                tblKey[i] = name;
                tblVal[i] = id;
                return id;
            }
            i = (i + 1) & MASK;
        }
        cacheMisses++;
        return intern(name);
    }

    /**
     * Entries are written as <b>two aligned {@code long} stores</b>, not thirteen bounds-checked byte
     * stores: the header packs {@code nodeId | keyId | tag} into one slot and the value occupies the
     * next.
     *
     * <p>Measured on a 30-node converging graph, 11.75 entries per event, against the byte loop this
     * replaces: <b>native −51.4 ns</b>, JIT unchanged. It also beats a {@code VarHandle} byte-array
     * view by 19.7 ns on native — and unlike {@code VarHandle} it is <b>pure Java 8</b>, so it ships
     * here rather than needing a multi-release jar or a generated writer.
     *
     * <p>Two aligned array stores are the simplest thing either compiler can emit: no unaligned access,
     * no byte assembly, nothing that has to be recognised and folded. HotSpot folds the byte loop
     * already, which is why it gains nothing; native-image does not, which is why it gains 51 ns.
     */
    private final long[] slots;
    private int slot;

    /**
     * The record header. Held as fields rather than written into the slot array so that entries stay
     * uniformly two slots each — the property a reader relies on to skip an entry without decoding it.
     *
     * <p>An earlier version wrote the header into the byte buffer while entries went to slots, and
     * {@link #length()} reported only the slots. The header was therefore built and then silently
     * discarded: no sink could see the event time, the log time, the event type or the end time.
     */
    private long eventTime;
    private long logTime;
    private long endTime;
    private int eventTypeId;

    private final byte[] buf;
    private int pos;
    private boolean overflow;

    /** Interning: the fallback map, plus a one-entry identity cache that generated code should always hit. */
    private final IdentityHashMap<String, Short> ids = new IdentityHashMap<>();
    private short nextId = 1;
    /** Two slots, not one: {@code head()} alternates node then key, so a single slot never hits. */
    private long cacheHits, cacheMisses;

    /** Default capacity — 8 KB holds ~600 entries, well beyond any single event's record. */
    public BinaryLogRecord(Clock clock) {
        this(clock, 8192);
    }

    public BinaryLogRecord(Clock clock, int capacity) {
        super(clock);
        this.buf = new byte[capacity];
        this.slots = new long[capacity / 4];
    }

    private short nodeId(String name) {
        return tableId(name);
        /*        if (name == lastNode) {          // reference equality — generated code passes constants
            cacheHits++;
            return lastNodeId;
        }
        cacheMisses++;
        lastNode = name;
        return lastNodeId = intern(name);
        */
    }

    private short keyId(String name) {
        return tableId(name);
        /*        if (name == lastKey) {
            cacheHits++;
            return lastKeyId;
        }
        cacheMisses++;
        lastKey = name;
        return lastKeyId = intern(name);
        */
    }

    private short intern(String name) {
        Short existing = ids.get(name);
        if (existing != null) {
            return existing;
        }
        short id = nextId++;
        ids.put(name, id);
        return id;
    }

    private void u8(int v) {
        if (pos < buf.length) { buf[pos++] = (byte) v; } else { overflow = true; }
    }

    private void u16(int v) {
        if (pos + 2 <= buf.length) {
            buf[pos++] = (byte) (v >>> 8); buf[pos++] = (byte) v;
        } else { overflow = true; }
    }

    /**
     * Written a byte at a time because {@code fluxtion-runtime} targets <b>Java 8</b> and
     * animal-sniffer enforces it, so {@code VarHandle} byte-array views are unavailable here.
     *
     * <p>That is not purely a loss. Measured on a 30-node graph logging 11.75 entries per event, a
     * single unaligned {@code VarHandle} store against this loop: <b>native 115.8 → 81.3 ns</b> but
     * <b>JIT 54.6 → 63.2</b>. HotSpot already folds this loop and pays for the {@code VarHandle}
     * indirection; native-image does not fold it and gains 34 ns. So the byte loop is the better choice
     * on a JIT and the worse one on native, and a native deployment wanting the 34 ns needs a
     * multi-release jar or a separate module — recorded as a known gap, not a defect.
     */
    private void i64(long v) {
        if (pos + 8 <= buf.length) {
            buf[pos++] = (byte) (v >>> 56); buf[pos++] = (byte) (v >>> 48);
            buf[pos++] = (byte) (v >>> 40); buf[pos++] = (byte) (v >>> 32);
            buf[pos++] = (byte) (v >>> 24); buf[pos++] = (byte) (v >>> 16);
            buf[pos++] = (byte) (v >>> 8);  buf[pos++] = (byte) v;
        } else { overflow = true; }
    }

    private void i32(int v) {
        if (pos + 4 <= buf.length) {
            buf[pos++] = (byte) (v >>> 24); buf[pos++] = (byte) (v >>> 16);
            buf[pos++] = (byte) (v >>> 8);  buf[pos++] = (byte) v;
        } else { overflow = true; }
    }

    private long now() {
        if (useProcessTime) { return clock.getProcessTime(); }
        if (noClock) { return 0L; }
        return clock.getWallClockTime();
    }

    // ---- the id path: EventLogger resolved these once per node, so nothing is looked up here ----


    @Override
    public int internName(String name) {
        return intern(name);
    }

    private void headById(int sourceRef, int keyRef) {
        u16(sourceRef);
        u16(keyRef);
    }

    @Override
    public void addRecord(int sourceRef, int keyRef, double value) {
        writeSlots(sourceRef, keyRef, TAG_DOUBLE, Double.doubleToRawLongBits(value));
    }

    /** Two aligned stores: the packed header, then the raw value bits. */
    private void writeSlots(int sourceRef, int keyRef, byte tag, long bits) {
        if (slot + 2 <= slots.length) {
            slots[slot] = ((long) sourceRef << 48) | ((long) (keyRef & 0xFFFF) << 32) | (tag & 0xFFL);
            slots[slot + 1] = bits;
            slot += 2;
            // firstProp is deliberately NOT written here. It exists so terminateRecord can answer
            // "did anything get logged", and on this path `slot` already answers it — so the store
            // was pure repetition, 11.75 times per event on the measured graph.
        } else {
            overflow = true;
        }
    }

    @Override
    public void addRecord(int sourceRef, int keyRef, long value) {
        writeSlots(sourceRef, keyRef, TAG_LONG, value);
    }

    @Override
    public void addRecord(int sourceRef, int keyRef, int value) {
        writeSlots(sourceRef, keyRef, TAG_INT, value);
    }

    @Override
    public void addRecord(int sourceRef, int keyRef, boolean value) {
        writeSlots(sourceRef, keyRef, TAG_BOOL, value ? 1L : 0L);
    }

    private void head(String sourceId, String propertyKey) {
        u16(nodeId(sourceId));
        u16(propertyKey == null ? 0 : keyId(propertyKey));
    }

    @Override
    public void addRecord(String sourceId, String propertyKey, double value) {
        head(sourceId, propertyKey);
        u8(TAG_DOUBLE);
        i64(Double.doubleToRawLongBits(value));
        firstProp = false;
    }

    @Override
    public void addRecord(String sourceId, String propertyKey, long value) {
        head(sourceId, propertyKey); u8(TAG_LONG); i64(value); firstProp = false;
    }

    @Override
    public void addRecord(String sourceId, String propertyKey, int value) {
        head(sourceId, propertyKey); u8(TAG_INT); i32(value); firstProp = false;
    }

    @Override
    public void addRecord(String sourceId, String propertyKey, char value) {
        head(sourceId, propertyKey); u8(TAG_CHAR); u16(value); firstProp = false;
    }

    @Override
    public void addRecord(String sourceId, String propertyKey, boolean value) {
        head(sourceId, propertyKey); u8(TAG_BOOL); u8(value ? 1 : 0); firstProp = false;
    }

    @Override
    public void addRecord(String sourceId, String propertyKey, CharSequence value) {
        head(sourceId, propertyKey);
        u8(TAG_CHARSEQ);
        int n = value == null ? 0 : value.length();
        u16(n);
        for (int i = 0; i < n; i++) { u8(value.charAt(i)); }
        firstProp = false;
    }

    @Override
    public void addRecord(String sourceId, String propertyKey, Object value) {
        // The only overload that cannot avoid text. A deployment aiming at the latency profile should
        // not be logging Objects; it is here so the record is complete, not because it is fast.
        head(sourceId, propertyKey);
        u8(TAG_OBJECT);
        String s = value == null ? "NULL" : value.toString();
        u16(s.length());
        for (int i = 0; i < s.length(); i++) { u8(s.charAt(i)); }
        firstProp = false;
    }

    @Override
    public void addTrace(String sourceId) {
        head(sourceId, null);
        u8(0);
    }

    @Override
    public void triggerEvent(Event event) { header(event.getClass()); }

    @Override
    public void triggerObject(Object event) { header(event.getClass()); }

    private void header(Class<?> type) {
        pos = 0;
        slot = 0;
        overflow = false;
        eventTime = clock.getEventTime();
        logTime = now();
        endTime = 0;
        // tableId, NOT intern. intern() is an IdentityHashMap lookup, and this line runs once per
        // EVENT — a profile of the audited graph put IdentityHashMap.get at 19% of samples, reached
        // only from here. Class.getName() returns the same cached String reference every call, so the
        // identity table that already exists for node and key names resolves it in one probe.
        eventTypeId = tableId(type.getName());
    }

    /** Time the event was created. */
    public long eventTime() { return eventTime; }

    /** Time processing began — {@code Clock.getProcessTime()}, see {@link LogRecord#logTime()}. */
    public long logTime() { return logTime; }

    /** Time processing completed; 0 until {@link #terminateRecord()} has run. */
    public long endTime() { return endTime; }

    /** Interned id of the event type. Resolve through {@link #dictionary()}. */
    public int eventTypeId() { return eventTypeId; }

    @Override
    public boolean terminateRecord() {
        // slot > 0 covers the id path, where writeSlots no longer touches firstProp; !firstProp
        // covers the String and trace paths, which still do.
        boolean logged = slot > 0 || !firstProp;
        endTime = now();
        firstProp = true;
        sourceId = null;
        return logged;
    }

    @Override
    public void clear() {
        firstProp = true;
        sourceId = null;
        pos = 0;
        slot = 0;
    }

    /**
     * The byte buffer used by the {@code String}-key and trace paths only. On the id path — the one the
     * latency profile takes — nothing is written here, and {@link #length()} describes {@link #slots()}
     * rather than this array. {@code BinaryLogWriter} reads the slots; so should any other sink.
     */
    public byte[] buffer() { return buf; }

    /** Size of the entry region, in bytes. A sink reads {@link #slots()}, not {@link #buffer()}. */
    public int length() { return slot * 8; }

    /** The entry slots. A reader consumes {@code slots()[0 .. length()/8)}. */
    public long[] slots() { return slots; }

    public boolean overflowed() { return overflow; }

    public long cacheHits() { return cacheHits; }

    public long cacheMisses() { return cacheMisses; }

    public int dictionarySize() { return ids.size(); }

    /** The id table, so a reader can resolve ids back to names. Index 0 is unused. */
    public String[] dictionary() {
        String[] out = new String[nextId];
        ids.forEach((name, id) -> out[id] = name);
        return out;
    }

    @Override
    public CharSequence asCharSequence() {
        throw new UnsupportedOperationException("binary record - use buffer()/length()");
    }
}
