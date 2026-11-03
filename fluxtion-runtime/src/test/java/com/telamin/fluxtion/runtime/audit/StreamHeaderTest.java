/*
 * Copyright: © 2026. Gregory Higgins
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.describe.ProcessorDescriptor;
import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The audit stream header: written once per stream, before the stream's first record, re-written for a new sink,
 * record format or host context, never written for a stream that publishes nothing; carried as a frame in a binary
 * file; and never handed to a binary sink that does not know it.
 */
public class StreamHeaderTest {

    static class Book extends EventLogNode {
        void publish(int qty) {
            auditLog.info("qty", qty);
        }
    }

    /** A descriptor recording a fingerprint, as a generated processor's does. */
    static final class Described implements ProcessorDescriptor {
        private final String fingerprint;

        Described(String fingerprint) {
            this.fingerprint = fingerprint;
        }

        @Override
        public String name() {
            return "com.acme.TradeProcessor";
        }

        @Override
        public String toolchainVersion() {
            return "1.0.84";
        }

        @Override
        public String sourceFingerprint() {
            return fingerprint;
        }

        @Override
        public List<Input> inputs() {
            return Collections.emptyList();
        }

        @Override
        public List<Sink> sinks() {
            return Collections.emptyList();
        }

        @Override
        public List<Service> services() {
            return Collections.emptyList();
        }

        @Override
        public Class<? extends DataFlow> dataFlowClass() {
            return null;
        }

        @Override
        public Supplier<? extends DataFlow> factory() {
            return null;
        }
    }

    private static Clock clock() {
        Clock c = new Clock();
        c.init();
        return c;
    }

    private static EventLogManager textManager(List<String> published) {
        EventLogManager m = new EventLogManager(r -> published.add(r.asCharSequence().toString()));
        m.clock = clock();
        m.init();
        return m;
    }

    private static void publish(EventLogManager m, Book book, int qty) {
        Object e = new Object();
        m.clock.eventReceived(e);
        m.eventReceived(e);
        book.publish(qty);
        m.processingComplete();
    }

    @Test
    public void theHeaderPrecedesTheFirstRecordAndIsWrittenOnce() {
        List<String> out = new ArrayList<>();
        EventLogManager m = textManager(out);
        m.setDescriptor(new Described("abc123"));
        Book book = new Book();
        m.nodeRegistered(book, "book");
        publish(m, book, 1);
        publish(m, book, 2);
        assertEquals("one header, two records: " + out, 3, out.size());
        assertTrue(out.get(0), out.get(0).startsWith("auditStreamHeader:"));
        assertTrue(out.get(0), out.get(0).contains("\n    schema: 1"));
        assertTrue(out.get(0), out.get(0).contains("\n    processor: com.acme.TradeProcessor"));
        assertTrue(out.get(0), out.get(0).contains("\n    sourceFingerprint: abc123"));
        assertTrue(out.get(0), out.get(0).contains("\n    toolchainVersion: 1.0.84"));
        assertTrue(out.get(0), out.get(0).contains("\n    host: {}"));
        assertTrue(out.get(1), out.get(1).startsWith("eventLogRecord:"));
        assertTrue(out.get(2), out.get(2).startsWith("eventLogRecord:"));
    }

    @Test
    public void aStreamThatPublishesNothingHasNoHeader() {
        List<String> out = new ArrayList<>();
        EventLogManager m = textManager(out);
        m.setDescriptor(new Described("abc123"));
        m.setHeaderContext(Collections.singletonMap("runId", "r1"));
        assertTrue("written at the first publish, never at init or on a setter: " + out, out.isEmpty());
    }

    @Test
    public void withNoDescriptorTheFingerprintIsNull() {
        List<String> out = new ArrayList<>();
        EventLogManager m = textManager(out);
        Book book = new Book();
        m.nodeRegistered(book, "book");
        publish(m, book, 1);
        assertTrue(out.get(0), out.get(0).contains("\n    processor: null"));
        assertTrue(out.get(0), out.get(0).contains("\n    sourceFingerprint: null"));
    }

    @Test
    public void aNewSinkANewContextAndAFormatSwapEachStartANewStream() {
        List<String> first = new ArrayList<>();
        EventLogManager m = textManager(first);
        Book book = new Book();
        m.nodeRegistered(book, "book");
        publish(m, book, 1);

        List<String> second = new ArrayList<>();
        m.setLogSink(r -> second.add(r.asCharSequence().toString()));
        publish(m, book, 2);
        assertTrue("a new sink gets its own header: " + second, second.get(0).startsWith("auditStreamHeader:"));

        Map<String, String> host = new LinkedHashMap<>();
        host.put("serverId", "pricing-1");
        host.put("runId", "2c41");
        host.put("group", "pricing");
        host.put("name", "quoter");
        host.put("generation", "1");
        m.setHeaderContext(host);
        assertEquals(host, m.getHeaderContext());
        publish(m, book, 3);
        assertEquals("header, record, header, record: " + second, 4, second.size());
        assertTrue(second.get(2), second.get(2).contains(
                "\n    host: {serverId: pricing-1, runId: 2c41, group: pricing, name: quoter, generation: '1'}"));

        List<String> third = new ArrayList<>();
        m.calculationLogConfig(new EventLogControlEvent(r -> third.add(r.asCharSequence().toString())));
        publish(m, book, 4);
        assertTrue("a sink installed by control event gets a header: " + third,
                third.get(0).startsWith("auditStreamHeader:"));
        assertEquals(1, first.stream().filter(s -> s.startsWith("auditStreamHeader:")).count());
    }

    @Test
    public void reapplyingTheSameContextDoesNotReHeadTheStream() {
        List<String> out = new ArrayList<>();
        EventLogManager m = textManager(out);
        m.setHeaderContext(Collections.singletonMap("runId", "r1"));
        Book book = new Book();
        m.nodeRegistered(book, "book");
        publish(m, book, 1);
        m.setHeaderContext(Collections.singletonMap("runId", "r1"));
        publish(m, book, 2);
        assertEquals("one header, two records: " + out, 3, out.size());
    }

    @Test
    public void theDescriptorTravelsWithASinkInstalledByControlEvent() {
        List<String> out = new ArrayList<>();
        EventLogManager m = new EventLogManager();
        m.clock = clock();
        m.init();
        Book book = new Book();
        m.nodeRegistered(book, "book");
        m.calculationLogConfig(new EventLogControlEvent(r -> out.add(r.asCharSequence().toString()))
                .descriptor(new Described("feed01")));
        publish(m, book, 1);
        assertTrue(out.get(0), out.get(0).contains("\n    sourceFingerprint: feed01"));
    }

    /**
     * Generated code registers its context with every auditor; the manager reads the processor's own descriptor
     * through it, so a processor from any builder names itself with no generated call and no host help.
     */
    @Test
    public void theProcessorsOwnDescriptorIsReadThroughItsRegisteredContext() {
        Described described = new Described("ctx777");
        Object[] self = new Object[1];
        Object processor = java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{DataFlow.class, com.telamin.fluxtion.runtime.callback.InternalEventProcessor.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getDescriptor": return described;
                        case "exportedService": return self[0];
                        case "hashCode": return 1;
                        case "equals": return proxy == args[0];
                        default: return method.getReturnType() == boolean.class ? false : null;
                    }
                });
        self[0] = processor;
        com.telamin.fluxtion.runtime.node.MutableDataFlowContext context =
                new com.telamin.fluxtion.runtime.node.MutableDataFlowContext(
                        null, new com.telamin.fluxtion.runtime.callback.CallbackDispatcherImpl(), null, null);
        context.setEventProcessorCallback((com.telamin.fluxtion.runtime.callback.InternalEventProcessor) processor);

        List<String> out = new ArrayList<>();
        EventLogManager m = textManager(out);
        m.nodeRegistered(context, "context");
        Book book = new Book();
        m.nodeRegistered(book, "book");
        publish(m, book, 1);
        assertTrue(out.get(0), out.get(0).contains("\n    processor: com.acme.TradeProcessor"));
        assertTrue(out.get(0), out.get(0).contains("\n    sourceFingerprint: ctx777"));
    }

    @Test
    public void aDescriptorSetOnTheManagerWinsOverTheProcessorsOwn() {
        List<String> out = new ArrayList<>();
        EventLogManager m = textManager(out);
        m.setDescriptor(new Described("host-set"));
        m.nodeRegistered(new com.telamin.fluxtion.runtime.node.MutableDataFlowContext(), "context");
        Book book = new Book();
        m.nodeRegistered(book, "book");
        publish(m, book, 1);
        assertTrue(out.get(0), out.get(0).contains("\n    sourceFingerprint: host-set"));
    }

    @Test
    public void aContextWithNoProcessorLeavesTheHeaderUnverifiedRatherThanFailing() {
        List<String> out = new ArrayList<>();
        EventLogManager m = textManager(out);
        m.nodeRegistered(new com.telamin.fluxtion.runtime.node.MutableDataFlowContext(), "context");
        Book book = new Book();
        m.nodeRegistered(book, "book");
        publish(m, book, 1);
        assertTrue(out.get(0), out.get(0).contains("\n    sourceFingerprint: null"));
        assertEquals(2, out.size());
    }

    @Test
    public void valuesThatYamlWouldMisreadAreQuoted() {
        Map<String, String> host = new LinkedHashMap<>();
        host.put("name", "a: b");
        host.put("flag", "true");
        host.put("quote", "it's");
        host.put("empty", "");
        String text = new AuditStreamHeader("p", null, null, null, "t", host).toString();
        assertTrue(text, text.contains("host: {name: 'a: b', flag: 'true', quote: 'it''s', empty: ''}"));
    }

    @Test
    public void aBinaryFileCarriesTheHeaderAsAFrameBeforeAnyRecord() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        EventLogManager m = new EventLogManager().binaryRecord(true);
        m.clock = clock();
        try (BinaryLogWriter writer = new BinaryLogWriter(bytes)) {
            m.setLogSink(writer);
            m.init();
            m.setDescriptor(new Described("abc123"));
            Book book = new Book();
            m.nodeRegistered(book, "book");
            publish(m, book, 1);
            publish(m, book, 2);
        }
        byte[] file = bytes.toByteArray();
        assertEquals("version 2 on the wire", 2, ((file[4] & 0xFF) << 8) | (file[5] & 0xFF));
        assertEquals("the first frame is the header", BinaryLogFile.FRAME_STREAM_HEADER, file[BinaryLogFile.HEADER_BYTES]);

        List<String> order = new ArrayList<>();
        BinaryLogReader.Result r = BinaryLogReader.read(file, new BinaryLogReader.Visitor() {
            @Override
            public void onStreamHeader(String headerText) {
                order.add("header");
            }

            @Override
            public boolean onRecord(int id, String type, long et, long lt, long end, int n) {
                order.add("record");
                return true;
            }

            @Override
            public void onEntry(int nodeId, String node, int keyId, String key, int tag, long rawBits) {
            }
        });
        assertEquals(2, r.formatVersion);
        assertEquals(2, r.records);
        assertEquals(1, r.streamHeaders.size());
        assertTrue(r.streamHeaders.get(0), r.streamHeaders.get(0).contains("sourceFingerprint: abc123"));
        assertEquals(java.util.Arrays.asList("header", "record", "record"), order);
    }

    @Test
    public void theStreamedReadPathDeliversTheSameHeader() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        EventLogManager m = new EventLogManager().binaryRecord(true);
        m.clock = clock();
        try (BinaryLogWriter writer = new BinaryLogWriter(bytes)) {
            m.setLogSink(writer);
            m.init();
            m.setDescriptor(new Described("abc123"));
            Book book = new Book();
            m.nodeRegistered(book, "book");
            publish(m, book, 1);
        }
        BinaryLogReader.Result whole = BinaryLogReader.read(bytes.toByteArray(), new Ignore());
        BinaryLogReader.Result streamed = BinaryLogReader.readStreamed(
                new java.io.ByteArrayInputStream(bytes.toByteArray()), 16, new Ignore());
        assertEquals(whole.streamHeaders, streamed.streamHeaders);
        assertEquals(whole.records, streamed.records);
        assertEquals(0, streamed.truncatedBytes);
    }

    @Test
    public void aBinarySinkThatDoesNotKnowTheHeaderNeverSeesIt() {
        List<Object> seen = new ArrayList<>();
        EventLogManager m = new EventLogManager(r -> seen.add((BinaryLogRecord) r)).binaryRecord(true);
        m.clock = clock();
        m.init();
        Book book = new Book();
        m.nodeRegistered(book, "book");
        publish(m, book, 1);
        assertEquals("a sink casting every record to BinaryLogRecord keeps working", 1, seen.size());
    }

    @Test
    public void aVersionOneFileStillReadsAndAHeaderFrameInItIsDamage() throws IOException {
        byte[] v1 = {'F', 'L', 'X', 'A', 0, 1, 0, 1};
        BinaryLogReader.Result r = BinaryLogReader.read(v1, new Ignore());
        assertEquals(1, r.formatVersion);
        assertEquals(0, r.records);

        byte[] v1WithHeader = {'F', 'L', 'X', 'A', 0, 1, 0, 1, BinaryLogFile.FRAME_STREAM_HEADER, 0, 0, 0, 0};
        try {
            BinaryLogReader.read(v1WithHeader, new Ignore());
            fail("a version-1 file never carried a header frame");
        } catch (IOException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("unknown frame type 0x3"));
        }
    }

    @Test
    public void aNewerVersionIsRefusedByNumber() {
        byte[] v3 = {'F', 'L', 'X', 'A', 0, 3, 0, 1};
        try {
            BinaryLogReader.read(v3, new Ignore());
            fail();
        } catch (IOException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("format version 3"));
        }
    }

    @Test
    public void theRuntimeVersionIsReadFromTheBuildStamp() {
        String v = AuditStreamHeader.runtimeVersionOnClasspath();
        assertFalse("the build stamp is filtered: " + v, v != null && v.startsWith("${"));
    }

    static final class Ignore implements BinaryLogReader.Visitor {
        @Override
        public boolean onRecord(int id, String type, long et, long lt, long end, int n) {
            return true;
        }

        @Override
        public void onEntry(int nodeId, String node, int keyId, String key, int tag, long rawBits) {
        }
    }
}
