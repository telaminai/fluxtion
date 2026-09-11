/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit.conformance;

import com.telamin.fluxtion.runtime.audit.BinaryLogFile;
import com.telamin.fluxtion.runtime.audit.BinaryLogRecord;
import com.telamin.fluxtion.runtime.audit.BinaryLogWriter;
import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;
import com.telamin.fluxtion.runtime.time.Clock;
import com.telamin.fluxtion.runtime.time.ClockStrategy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The FLXA conformance corpus — one fixture per pinned semantic of the binary audit format, as the
 * specification ({@code docs/reference/flxa-format.md}) defines them.
 *
 * <p>Every fixture is produced by THIS class, deterministically, from the shipped {@link BinaryLogWriter}
 * and a counting clock; the "derived" fixtures (a wrong unit code, a truncated tail, an undefined tag)
 * are byte edits of a produced one, so they too are reproducible. The bytes are also committed under
 * {@code src/main/resources/…/conformance/} and ship in the runtime jar, so a consumer in another
 * repository or language reads the same bytes this class would generate. {@code FlxaConformanceTest}
 * asserts the two agree; if the writer's output changes, that test — not a consumer — is what breaks.
 *
 * <p><b>What passing means.</b> A writer conforms when, for the fixtures marked as written, it produces
 * these bytes from these inputs. A reader conforms when it reports what the specification's table says
 * each fixture must be read as — including which fixtures it must refuse, and before delivering what.
 * A text constructor (the analyser) conforms when the text it builds from each fixture parses back to
 * the same entries, with nothing manufactured and nothing lost.
 */
public final class FlxaConformanceCorpus {

    /** A millisecond epoch base — 2023-11-14T22:13:20Z — so magnitudes read as real timestamps. */
    public static final long MILLIS_BASE = 1_700_000_000_000L;
    /** The same instant in nanoseconds. */
    public static final long NANOS_BASE = 1_700_000_000_000_000_000L;

    /** The event type most fixtures use. Its simple name is {@code Tick}. */
    public static final class Tick { }

    /** A second event type with the SAME simple name and a different identity. */
    public static final class Other {
        public static final class Tick { }
    }

    /** An Object value with a stable {@code toString()}. */
    static final class Obj {
        @Override
        public String toString() { return "Obj(1)"; }
    }

    private static final Map<String, Supplier<byte[]>> FIXTURES = new LinkedHashMap<>();

    static {
        FIXTURES.put("f01-minimal", FlxaConformanceCorpus::minimal);
        FIXTURES.put("f02-empty-record", FlxaConformanceCorpus::emptyRecord);
        FIXTURES.put("f03-every-tag", FlxaConformanceCorpus::everyTag);
        FIXTURES.put("f04-null-values", FlxaConformanceCorpus::nullValues);
        FIXTURES.put("f05-dictionary-growth", FlxaConformanceCorpus::dictionaryGrowth);
        FIXTURES.put("f06-two-records-two-dictionaries", FlxaConformanceCorpus::twoRecordsTwoDictionaries);
        FIXTURES.put("f07-truncated-tail", FlxaConformanceCorpus::truncatedTail);
        FIXTURES.put("f08-unit-nanos", FlxaConformanceCorpus::unitNanos);
        FIXTURES.put("f09-unit-unspecified", () -> withUnit(minimal(), BinaryLogFile.TIME_UNIT_UNSPECIFIED));
        FIXTURES.put("f10-unit-undefined", () -> withUnit(minimal(), 3));
        FIXTURES.put("f11-unresolved-ids", FlxaConformanceCorpus::unresolvedIds);
        FIXTURES.put("f12-unknown-value-tag", FlxaConformanceCorpus::unknownValueTag);
        FIXTURES.put("f13-hostile-strings", FlxaConformanceCorpus::hostileStrings);
        FIXTURES.put("f14-hostile-chars", FlxaConformanceCorpus::hostileChars);
        FIXTURES.put("f15-same-simple-name", FlxaConformanceCorpus::sameSimpleName);
        FIXTURES.put("f16-unknown-frame", FlxaConformanceCorpus::unknownFrame);
        FIXTURES.put("f17-unresolved-value-ids", FlxaConformanceCorpus::unresolvedValueIds);
        FIXTURES.put("f18-duplicate-dict-id", FlxaConformanceCorpus::duplicateDictId);
        FIXTURES.put("f19-malformed-utf8", FlxaConformanceCorpus::malformedUtf8);
    }

    private FlxaConformanceCorpus() {
    }

    /** Fixture names, in specification order. */
    public static List<String> names() {
        return Collections.unmodifiableList(Arrays.asList(FIXTURES.keySet().toArray(new String[0])));
    }

    /** The fixture's bytes as this class generates them now. */
    public static byte[] generate(String name) {
        Supplier<byte[]> s = FIXTURES.get(name);
        if (s == null) {
            throw new IllegalArgumentException("no fixture " + name + "; names are " + names());
        }
        return s.get();
    }

    /** The fixture's bytes as committed and shipped in the runtime jar. */
    public static byte[] committed(String name) {
        String resource = "/com/telamin/fluxtion/runtime/audit/conformance/" + name + ".flxa";
        try (InputStream in = FlxaConformanceCorpus.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("committed fixture missing from the jar: " + resource);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Writes every fixture into {@code args[0]} — how the committed bytes are (re)produced. */
    public static void main(String[] args) throws IOException {
        Path dir = Paths.get(args[0]);
        Files.createDirectories(dir);
        for (String name : names()) {
            Files.write(dir.resolve(name + ".flxa"), generate(name));
        }
    }

    // ------------------------------------------------------------------ the producing environment

    /** A clock whose every reading is the previous one plus one, from {@code base}. */
    private static Clock countingClock(long base) {
        long[] next = {base};
        Clock clock = new Clock();
        clock.init();
        clock.setClockStrategy(new ClockStrategy.ClockStrategyEvent(() -> next[0]++));
        return clock;
    }

    private static BinaryLogRecord record(Clock clock) {
        BinaryLogRecord r = new BinaryLogRecord(clock, 4096);
        r.updateLogLevel(LogLevel.INFO);
        return r;
    }

    /** Starts a record for {@code event}: the clock reads once for the arrival, as the processor does. */
    private static void arrive(Clock clock, BinaryLogRecord r, Object event) {
        clock.eventReceived(event);
        r.triggerObject(event);
    }

    private static byte[] write(int timeUnit, WriterBody body) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (BinaryLogWriter w = new BinaryLogWriter(out, timeUnit)) {
            body.write(w);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private interface WriterBody {
        void write(BinaryLogWriter w) throws IOException;
    }

    // ------------------------------------------------------------------ produced fixtures

    /** One record, one node, one double. The smallest useful file. */
    static byte[] minimal() {
        Clock clock = countingClock(MILLIS_BASE);
        return write(BinaryLogFile.TIME_UNIT_EPOCH_MILLIS, w -> {
            BinaryLogRecord r = record(clock);
            arrive(clock, r, new Tick());
            r.addRecord("pricer", "price", 1.25d);
            r.terminateRecord();
            w.processLogRecord(r);
        });
    }

    /** A record with no entries still happened. */
    static byte[] emptyRecord() {
        Clock clock = countingClock(MILLIS_BASE);
        return write(BinaryLogFile.TIME_UNIT_EPOCH_MILLIS, w -> {
            BinaryLogRecord r = record(clock);
            arrive(clock, r, new Tick());
            r.terminateRecord();
            w.processLogRecord(r);
        });
    }

    /** Every value tag the format defines, in tag order, plus a trace entry. */
    static byte[] everyTag() {
        Clock clock = countingClock(MILLIS_BASE);
        return write(BinaryLogFile.TIME_UNIT_EPOCH_MILLIS, w -> {
            BinaryLogRecord r = record(clock);
            arrive(clock, r, new Tick());
            r.addRecord("node", "aDouble", 1.5d);
            r.addRecord("node", "aLong", -7L);
            r.addRecord("node", "anInt", 42);
            r.addRecord("node", "aChar", 'x');
            r.addRecord("node", "aString", (CharSequence) "text");
            r.addRecord("node", "anObject", new Obj());
            r.addRecord("node", "aBool", true);
            r.addTrace("tracer");
            r.terminateRecord();
            w.processLogRecord(r);
        });
    }

    /** A logged null String and a logged null Object: dictionary id 0. */
    static byte[] nullValues() {
        Clock clock = countingClock(MILLIS_BASE);
        return write(BinaryLogFile.TIME_UNIT_EPOCH_MILLIS, w -> {
            BinaryLogRecord r = record(clock);
            arrive(clock, r, new Tick());
            r.addRecord("node", "nullString", (CharSequence) null);
            r.addRecord("node", "nullObject", (Object) null);
            r.addRecord("node", "present", (CharSequence) "here");
            r.terminateRecord();
            w.processLogRecord(r);
        });
    }

    /** Names first used in the second record are defined between the records. */
    static byte[] dictionaryGrowth() {
        Clock clock = countingClock(MILLIS_BASE);
        return write(BinaryLogFile.TIME_UNIT_EPOCH_MILLIS, w -> {
            BinaryLogRecord r = record(clock);
            arrive(clock, r, new Tick());
            r.addRecord("pricer", "price", 1.25d);
            r.terminateRecord();
            w.processLogRecord(r);
            arrive(clock, r, new Tick());
            r.addRecord("pricer", "price", 1.5d);
            r.addRecord("risk", "breach", true);
            r.addRecord("risk", "reason", (CharSequence) "limit");
            r.terminateRecord();
            w.processLogRecord(r);
        });
    }

    /**
     * Two record INSTANCES whose record-scoped ids disagree: the second interns the same names in a
     * different order. The file's ids are by name, so both records attribute {@code price} to
     * {@code pricer} — a reader that trusted record-scoped ids would attribute the second to {@code risk}.
     */
    static byte[] twoRecordsTwoDictionaries() {
        Clock clock = countingClock(MILLIS_BASE);
        return write(BinaryLogFile.TIME_UNIT_EPOCH_MILLIS, w -> {
            BinaryLogRecord a = record(clock);
            arrive(clock, a, new Tick());
            a.addRecord("pricer", "price", 1.25d);
            a.terminateRecord();
            w.processLogRecord(a);

            BinaryLogRecord b = record(clock);
            b.internName("risk");          // record-scoped id 1 in b is "risk"; in a it was the event type
            b.internName("breach");
            arrive(clock, b, new Tick());
            b.addRecord("pricer", "price", 2.5d);
            b.addRecord("risk", "breach", false);
            b.terminateRecord();
            w.processLogRecord(b);
        });
    }

    /** Two records, the second cut mid-frame: a crash's normal end state. */
    static byte[] truncatedTail() {
        byte[] whole = dictionaryGrowth();
        return Arrays.copyOf(whole, whole.length - 5);
    }

    /** Written under a nanosecond strategy with the unit declared. */
    static byte[] unitNanos() {
        Clock clock = countingClock(NANOS_BASE);
        return write(BinaryLogFile.TIME_UNIT_EPOCH_NANOS, w -> {
            BinaryLogRecord r = record(clock);
            arrive(clock, r, new Tick());
            r.addRecord("pricer", "price", 1.25d);
            r.terminateRecord();
            w.processLogRecord(r);
        });
    }

    /** A header, then a record emitted through {@code encodeTo} with NO dictionary: every id unresolved. */
    static byte[] unresolvedIds() {
        Clock clock = countingClock(MILLIS_BASE);
        BinaryLogRecord r = record(clock);
        arrive(clock, r, new Tick());
        r.addRecord("pricer", "price", 1.25d);
        r.terminateRecord();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            new BinaryLogWriter(out).close();   // header only
            r.encodeTo(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    /** The minimal file with its one entry's tag byte set to 9, which the format does not define. */
    static byte[] unknownValueTag() {
        byte[] bytes = minimal();
        int record = firstRecordOffset(bytes);
        // slot0 is big-endian; the tag is its low byte, i.e. the last of the eight.
        bytes[record + BinaryLogFile.RECORD_FIXED_BYTES + 7] = 9;
        return bytes;
    }

    /** Strings, keys and node names that are syntax to any text they are written into bare. */
    static byte[] hostileStrings() {
        Clock clock = countingClock(MILLIS_BASE);
        return write(BinaryLogFile.TIME_UNIT_EPOCH_MILLIS, w -> {
            BinaryLogRecord r = record(clock);
            arrive(clock, r, new Tick());
            r.addRecord("pricer", "status", (CharSequence) "ok, price: 42.0");
            r.addRecord("pricer", "identity", (CharSequence) "x}\n  eventType: forged.Tick\n  endTime: 1");
            r.addRecord("pricer", "nullText", (CharSequence) "null");
            r.addRecord("pricer", "numberText", (CharSequence) "42.0");
            r.addRecord("pricer", "flagText", (CharSequence) "true");
            r.addRecord("pricer", "empty", (CharSequence) "");
            r.addRecord("pricer", "padded", (CharSequence) " x ");
            r.addRecord("pricer", "quotes", (CharSequence) "say \"hi\" \\ done");
            r.addRecord("pricer", "plain", (CharSequence) "NEW");
            r.addRecord("odd}: {node", "a, b: c", (CharSequence) "1");
            r.addRecord("pricer", "price", 7.0d);
            r.terminateRecord();
            w.processLogRecord(r);
        });
    }

    /** Characters that are syntax, each followed by a figure that must survive. */
    static byte[] hostileChars() {
        Clock clock = countingClock(MILLIS_BASE);
        return write(BinaryLogFile.TIME_UNIT_EPOCH_MILLIS, w -> {
            BinaryLogRecord r = record(clock);
            arrive(clock, r, new Tick());
            r.addRecord("n", "quote", '\'');
            r.addRecord("n", "afterQuote", 1.0d);
            r.addRecord("n", "brace", '{');
            r.addRecord("n", "afterBrace", 2.0d);
            r.addRecord("n", "dquote", '"');
            r.addRecord("n", "afterDquote", 3.0d);
            r.addRecord("n", "digit", '7');
            r.addRecord("n", "afterDigit", 4.0d);
            r.addRecord("n", "newline", '\n');
            r.addRecord("n", "afterNewline", 5.0d);
            r.terminateRecord();
            w.processLogRecord(r);
        });
    }

    /** Two event types with one simple name: the file records the full identity of each. */
    static byte[] sameSimpleName() {
        Clock clock = countingClock(MILLIS_BASE);
        return write(BinaryLogFile.TIME_UNIT_EPOCH_MILLIS, w -> {
            BinaryLogRecord r = record(clock);
            arrive(clock, r, new Tick());
            r.addRecord("pricer", "price", 1.0d);
            r.terminateRecord();
            w.processLogRecord(r);
            arrive(clock, r, new Other.Tick());
            r.addRecord("pricer", "price", 2.0d);
            r.terminateRecord();
            w.processLogRecord(r);
        });
    }

    /** The minimal file followed by a frame tag the format does not define. */
    static byte[] unknownFrame() {
        byte[] bytes = minimal();
        byte[] out = Arrays.copyOf(bytes, bytes.length + 1);
        out[bytes.length] = 0x07;
        return out;
    }

    /**
     * Every STRUCTURAL id resolves; the two VALUE ids (a String and an Object) point at ids the file
     * never defines. A reader that counts only event/node/key ids reports this file whole.
     */
    static byte[] unresolvedValueIds() {
        Clock clock = countingClock(MILLIS_BASE);
        byte[] bytes = write(BinaryLogFile.TIME_UNIT_EPOCH_MILLIS, w -> {
            BinaryLogRecord r = record(clock);
            arrive(clock, r, new Tick());
            r.addRecord("node", "aString", (CharSequence) "text");
            r.addRecord("node", "anObject", new Obj());
            r.addRecord("node", "aDouble", 1.25d);
            r.terminateRecord();
            w.processLogRecord(r);
        });
        patchValueId(bytes, 0, 65000);
        patchValueId(bytes, 1, 65001);
        return bytes;
    }

    /** The minimal file with a second DICT frame redefining id 2 ("pricer") before the record uses it. */
    static byte[] duplicateDictId() {
        byte[] bytes = minimal();
        int record = firstRecordOffset(bytes);
        byte[] name = "renamed".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] frame = new byte[5 + name.length];
        frame[0] = (byte) BinaryLogFile.FRAME_DICT;
        frame[1] = 0; frame[2] = 2;                       // id 2
        frame[3] = 0; frame[4] = (byte) name.length;
        System.arraycopy(name, 0, frame, 5, name.length);
        byte[] out = new byte[bytes.length + frame.length];
        System.arraycopy(bytes, 0, out, 0, record);
        System.arraycopy(frame, 0, out, record, frame.length);
        System.arraycopy(bytes, record, out, record + frame.length, bytes.length - record);
        return out;
    }

    /** The minimal file with the first byte of the name "pricer" replaced by 0xFF, which is not UTF-8. */
    static byte[] malformedUtf8() {
        byte[] bytes = minimal();
        int at = indexOf(bytes, "pricer".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        bytes[at] = (byte) 0xFF;
        return bytes;
    }

    // ------------------------------------------------------------------ byte edits

    /** Overwrites slot1 of the given entry of the first RECORD frame with {@code id}. */
    static void patchValueId(byte[] bytes, int entryIndex, long id) {
        int at = firstRecordOffset(bytes) + BinaryLogFile.RECORD_FIXED_BYTES + entryIndex * 16 + 8;
        for (int i = 7; i >= 0; i--) {
            bytes[at + i] = (byte) id;
            id >>>= 8;
        }
    }

    static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i + needle.length <= haystack.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        throw new IllegalStateException("needle not found");
    }

    static byte[] withUnit(byte[] bytes, int code) {
        byte[] out = bytes.clone();
        out[6] = (byte) (code >>> 8);
        out[7] = (byte) code;
        return out;
    }

    /** Walks dictionary frames from the header to the first RECORD frame. */
    static int firstRecordOffset(byte[] bytes) {
        int p = BinaryLogFile.HEADER_BYTES;
        while (p < bytes.length && bytes[p] == BinaryLogFile.FRAME_DICT) {
            int len = ((bytes[p + 3] & 0xFF) << 8) | (bytes[p + 4] & 0xFF);
            p += 5 + len;
        }
        if (p >= bytes.length || bytes[p] != BinaryLogFile.FRAME_RECORD) {
            throw new IllegalStateException("no RECORD frame at " + p);
        }
        return p;
    }
}
