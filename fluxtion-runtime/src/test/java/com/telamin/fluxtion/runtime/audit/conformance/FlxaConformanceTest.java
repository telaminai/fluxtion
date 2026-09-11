package com.telamin.fluxtion.runtime.audit.conformance;

import com.telamin.fluxtion.runtime.audit.BinaryLogFile;
import com.telamin.fluxtion.runtime.audit.BinaryLogReader;
import com.telamin.fluxtion.runtime.audit.BinaryRecordDecoder;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * The FLXA specification's conformance suite for the JAVA writer and reader — one test per pinned
 * semantic, named for its fixture. {@code docs/reference/flxa-format.md} is the normative text; this
 * is what passing it means for this runtime. The analyser has the reader-and-text half of the same
 * suite over the same bytes, loaded from this jar.
 *
 * <p>Two things are asserted about every fixture before its semantics: the committed bytes equal what
 * {@link FlxaConformanceCorpus} generates now (WRITER conformance — the shipped writer still produces
 * the specified bytes), and the header parses.
 */
public class FlxaConformanceTest {

    /** Collects everything the reader delivers, in order, as strings a test can assert on. */
    static final class Recording implements BinaryLogReader.Visitor {
        final List<String> events = new ArrayList<>();
        int headerUnit = -1;
        final List<String> records = new ArrayList<>();
        final List<String> entries = new ArrayList<>();
        final List<String> dictionary = new ArrayList<>();

        @Override public void onHeader(int version, int unit) { headerUnit = unit; events.add("header"); }
        @Override public void onDictionaryEntry(int id, String name) { dictionary.add(id + "=" + name); events.add("dict"); }
        @Override public boolean onRecord(int typeId, String type, long ev, long log, long end, int n) {
            records.add(type + " ev=" + ev + " log=" + log + " end=" + end + " n=" + n);
            events.add("record");
            return true;
        }
        @Override public void onEntry(int nodeId, String node, int keyId, String key, int tag, long bits) {
            entries.add(node + "." + key + " tag=" + tag + " " + BinaryRecordDecoder.renderValue(tag, bits, id -> nameById(id)));
            events.add("entry");
        }
        private String nameById(int id) {
            for (String d : dictionary) {
                if (d.startsWith(id + "=")) return d.substring(d.indexOf('=') + 1);
            }
            return null;
        }
    }

    private static Recording read(String fixture) throws IOException {
        Recording r = new Recording();
        BinaryLogReader.read(FlxaConformanceCorpus.committed(fixture), r);
        return r;
    }

    private static BinaryLogReader.Result result(String fixture) throws IOException {
        return BinaryLogReader.read(FlxaConformanceCorpus.committed(fixture), new Recording());
    }

    private static final String TICK = FlxaConformanceCorpus.Tick.class.getName();

    // ------------------------------------------------------------------ writer conformance

    @Test
    public void everyCommittedFixtureIsWhatTheShippedWriterProducesNow() {
        for (String name : FlxaConformanceCorpus.names()) {
            assertArrayEquals("fixture " + name + " drifted: regenerate with FlxaConformanceCorpus.main and "
                    + "treat the diff as a FORMAT change to be specified, not a file to be refreshed",
                    FlxaConformanceCorpus.generate(name), FlxaConformanceCorpus.committed(name));
        }
        assertEquals("the set is the published artefact; add a fixture here AND a test below", 19,
                FlxaConformanceCorpus.names().size());
    }

    @Test
    public void everyFixtureStartsWithTheHeader() {
        for (String name : FlxaConformanceCorpus.names()) {
            byte[] b = FlxaConformanceCorpus.committed(name);
            assertTrue(name, b.length >= BinaryLogFile.HEADER_BYTES);
            assertArrayEquals(name, BinaryLogFile.MAGIC, Arrays.copyOf(b, 4));
            assertEquals(name + " version", BinaryLogFile.FORMAT_VERSION, ((b[4] & 0xFF) << 8) | (b[5] & 0xFF));
        }
    }

    // ------------------------------------------------------------------ reader conformance, per fixture

    @Test
    public void f01_minimal_oneRecordOneEntry_everyIdDefinedBeforeUse() throws IOException {
        Recording r = read("f01-minimal");
        assertEquals("header first, then every name, then the record", "header", r.events.get(0));
        assertEquals(Arrays.asList("1=" + TICK, "2=pricer", "3=price"), r.dictionary);
        assertEquals(1, r.records.size());
        assertEquals("arrival reading for eventTime and logTime, a second reading for endTime",
                TICK + " ev=1700000000000 log=1700000000000 end=1700000000001 n=1", r.records.get(0));
        assertEquals(Arrays.asList("pricer.price tag=1 1.25"), r.entries);
        assertEquals(BinaryLogFile.TIME_UNIT_EPOCH_MILLIS, r.headerUnit);
        assertEquals(0, result("f01-minimal").unresolvedIds);
        assertEquals(0, result("f01-minimal").truncatedBytes);
    }

    @Test
    public void f02_emptyRecord_isARecord() throws IOException {
        Recording r = read("f02-empty-record");
        assertEquals(1, r.records.size());
        assertTrue(r.records.get(0).endsWith(" n=0"));
        assertEquals(0, r.entries.size());
    }

    @Test
    public void f03_everyTag_decodesAsSpecified() throws IOException {
        Recording r = read("f03-every-tag");
        assertEquals(Arrays.asList(
                "node.aDouble tag=1 1.5",
                "node.aLong tag=2 -7",
                "node.anInt tag=3 42",
                "node.aChar tag=4 x",
                "node.aString tag=5 text",
                "node.anObject tag=6 Obj(1)",
                "node.aBool tag=7 true",
                "tracer.null tag=8 "), r.entries);
        assertEquals("a trace entry has key id 0 and is not an unresolved id", 0, result("f03-every-tag").unresolvedIds);
    }

    @Test
    public void f04_nullValues_areDictionaryIdZero() throws IOException {
        Recording r = read("f04-null-values");
        assertEquals(Arrays.asList("node.nullString tag=5 null", "node.nullObject tag=6 null", "node.present tag=5 here"), r.entries);
        assertEquals("id 0 is null, not unresolved", 0, result("f04-null-values").unresolvedIds);
    }

    @Test
    public void f05_dictionaryGrowth_namesAreDefinedBeforeTheRecordThatUsesThem() throws IOException {
        Recording r = read("f05-dictionary-growth");
        List<String> order = r.events;
        int firstRecord = order.indexOf("record");
        int secondRecord = order.lastIndexOf("record");
        assertEquals("three names before the first record", 3, order.subList(0, firstRecord).stream().filter("dict"::equals).count());
        assertEquals("four more between the records: two nodes, a key, and a String VALUE", 4,
                order.subList(firstRecord, secondRecord).stream().filter("dict"::equals).count());
        assertEquals(Arrays.asList("pricer.price tag=1 1.25", "pricer.price tag=1 1.5", "risk.breach tag=7 true", "risk.reason tag=5 limit"), r.entries);
    }

    @Test
    public void f06_twoRecordsTwoDictionaries_fileIdsAreByName() throws IOException {
        Recording r = read("f06-two-records-two-dictionaries");
        assertEquals(Arrays.asList("pricer.price tag=1 1.25", "pricer.price tag=1 2.5", "risk.breach tag=7 false"), r.entries);
        assertEquals("each name defined once: Tick, pricer, price, risk, breach", 5, r.dictionary.size());
        assertEquals(0, result("f06-two-records-two-dictionaries").unresolvedIds);
    }

    @Test
    public void f07_truncatedTail_everyWholeFrameIsDeliveredAndTheRestIsReported() throws IOException {
        BinaryLogReader.Result res = result("f07-truncated-tail");
        assertEquals("the first record, whole, is delivered", 1, res.records);
        assertTrue("the cut record's bytes are reported, not swallowed: " + res.truncatedBytes, res.truncatedBytes > 0);
        assertEquals("the seven names before the cut are still delivered", 7,
                res.dictionary.stream().filter(java.util.Objects::nonNull).count());
    }

    @Test
    public void f08_unitNanos_headerSaysSoBeforeAnyRecord() throws IOException {
        Recording r = read("f08-unit-nanos");
        assertEquals(BinaryLogFile.TIME_UNIT_EPOCH_NANOS, r.headerUnit);
        assertEquals("header", r.events.get(0));
        assertTrue(r.records.get(0), r.records.get(0).contains("log=1700000000000000000"));
    }

    @Test
    public void f09_unitUnspecified_isDeliveredAsCodeZero_whichTheExplicitWriterMayState() throws IOException {
        assertEquals(BinaryLogFile.TIME_UNIT_UNSPECIFIED, read("f09-unit-unspecified").headerUnit);
        // 0 is a DEFINED code: a writer that states no unit may say so through the explicit-unit
        // constructor, and the default constructor writes 1. What no writer may do is write an
        // UNDEFINED code - f10 exists only by byte edit.
        assertTrue(BinaryLogFile.isKnownTimeUnit(0));
        assertFalse(BinaryLogFile.isKnownTimeUnit(3));
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        new com.telamin.fluxtion.runtime.audit.BinaryLogWriter(out, 0);
        assertEquals("the explicit-unit constructor writes 0 when told to", 0, out.toByteArray()[7]);
    }

    @Test
    public void f10_unitUndefined_theRuntimeReaderDeliversTheCodeAndDecidesNothing() throws IOException {
        Recording r = read("f10-unit-undefined");
        assertEquals("the reader passes the code on; which codes to accept is the consumer's policy", 3, r.headerUnit);
        assertEquals(1, r.records.size());
    }

    @Test
    public void f11_unresolvedIds_areRenderedAsHashIdAndCounted_neverThrown() throws IOException {
        Recording r = read("f11-unresolved-ids");
        assertEquals(0, r.dictionary.size());
        assertEquals(1, r.records.size());
        assertTrue(r.records.get(0), r.records.get(0).startsWith("#1 "));
        assertEquals(Arrays.asList("#2.#3 tag=1 1.25"), r.entries);
        assertEquals("event type, node, key", 3, result("f11-unresolved-ids").unresolvedIds);
    }

    @Test
    public void f12_unknownValueTag_isDeliveredWithItsBitsAndRenderedDiagnostically() throws IOException {
        Recording r = read("f12-unknown-value-tag");
        assertEquals(1, r.entries.size());
        assertTrue(r.entries.get(0), r.entries.get(0).startsWith("pricer.price tag=9 #tag9:"));
        assertFalse(BinaryRecordDecoder.knownTag(9));
    }

    @Test
    public void f13_hostileStrings_roundTripExactly() throws IOException {
        Recording r = read("f13-hostile-strings");
        assertEquals(11, r.entries.size());
        assertEquals("pricer.status tag=5 ok, price: 42.0", r.entries.get(0));
        assertEquals("pricer.identity tag=5 x}\n  eventType: forged.Tick\n  endTime: 1", r.entries.get(1));
        assertEquals("pricer.nullText tag=5 null", r.entries.get(2));
        assertEquals("pricer.empty tag=5 ", r.entries.get(5));
        assertEquals("odd}: {node.a, b: c tag=5 1", r.entries.get(9));
        assertEquals("pricer.price tag=1 7.0", r.entries.get(10));
    }

    @Test
    public void f14_hostileChars_roundTripWithTheirFollowingFigures() throws IOException {
        Recording r = read("f14-hostile-chars");
        assertEquals(10, r.entries.size());
        assertEquals("n.quote tag=4 '", r.entries.get(0));
        assertEquals("n.afterQuote tag=1 1.0", r.entries.get(1));
        assertEquals("n.digit tag=4 7", r.entries.get(6));
        assertEquals("n.newline tag=4 \n", r.entries.get(8));
    }

    @Test
    public void f15_sameSimpleName_theFileRecordsFullIdentity() throws IOException {
        Recording r = read("f15-same-simple-name");
        assertEquals(2, r.records.size());
        assertTrue(r.records.get(0).startsWith(FlxaConformanceCorpus.Tick.class.getName() + " "));
        assertTrue(r.records.get(1).startsWith(FlxaConformanceCorpus.Other.Tick.class.getName() + " "));
        assertNotEquals(r.records.get(0).split(" ")[0], r.records.get(1).split(" ")[0]);
    }

    @Test
    public void f17_unresolvedValueIds_areCountedLikeEveryOtherRole() throws IOException {
        Recording r = read("f17-unresolved-value-ids");
        assertEquals(Arrays.asList("node.aString tag=5 #65000", "node.anObject tag=6 #65001", "node.aDouble tag=1 1.25"), r.entries);
        assertEquals("two occurrences, both values; every structural id resolved", 2,
                result("f17-unresolved-value-ids").unresolvedIds);
    }

    @Test
    public void f18_duplicateDictId_theLatestDefinitionNamesWhatFollows() throws IOException {
        Recording r = read("f18-duplicate-dict-id");
        assertEquals("both definitions are delivered, in order",
                Arrays.asList("1=" + TICK, "2=pricer", "3=price", "2=renamed"), r.dictionary);
        assertEquals("the record after the redefinition uses it", Arrays.asList("renamed.price tag=1 1.25"), r.entries);
        assertEquals(0, result("f18-duplicate-dict-id").unresolvedIds);
    }

    @Test
    public void f19_malformedUtf8_isReplacedNeverFatal() throws IOException {
        Recording r = read("f19-malformed-utf8");
        assertEquals(1, r.entries.size());
        assertTrue("the bad byte becomes U+FFFD and the rest of the name survives: " + r.entries.get(0),
                r.entries.get(0).startsWith("\uFFFDricer.price tag=1 1.25"));
        assertEquals(0, result("f19-malformed-utf8").unresolvedIds);
    }

    @Test
    public void f16_unknownFrame_framesBeforeItAreDelivered_thenItIsReported() {
        Recording r = new Recording();
        try {
            BinaryLogReader.read(FlxaConformanceCorpus.committed("f16-unknown-frame"), r);
            fail("an undefined frame tag has no length and cannot be skipped");
        } catch (IOException reported) {
            assertTrue(reported.getMessage(), reported.getMessage().contains("unknown frame type 0x7"));
        }
        assertEquals("the record before it was delivered", 1, r.records.size());
    }
}
