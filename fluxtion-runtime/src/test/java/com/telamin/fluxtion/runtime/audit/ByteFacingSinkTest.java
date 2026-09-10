package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * A sink can take a record's bytes without knowing which kind of record it is (M52.3).
 *
 * <p>spec-binary-audit-encoding §6.1(2). Before this, {@code asCharSequence()} was the only expression
 * channel: a binary record threw from it, so any sink wanting the bytes had to downcast to a vendor
 * class — which the spec named as the thing a vendor sink should not have to do.
 *
 * <p><b>The test is written as the sink an integrator would actually write</b>: it holds a
 * {@code LogRecord}, knows nothing else about it, and asks for bytes. If that required a downcast this
 * would not compile.
 */
public class ByteFacingSinkTest {

    /** Ships bytes onward and never asks what kind of record produced them. */
    private static final class ForwardingSink implements LogRecordListener {
        final ByteArrayOutputStream captured = new ByteArrayOutputStream();
        int records;

        @Override
        public void processLogRecord(LogRecord logRecord) {
            try {
                records++;
                processLogRecord(logRecord, captured);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static final class TickEvent {
    }

    @Test
    public void aBinaryRecordHandsOverItsFrameWithNoDowncast() {
        BinaryLogRecord record = new BinaryLogRecord(new Clock());
        int node = record.internName("pricer");
        int key = record.internName("price");
        record.triggerObject(new TickEvent());
        record.addRecord(node, key, 42);

        ForwardingSink sink = new ForwardingSink();
        sink.processLogRecord(record);

        byte[] bytes = sink.captured.toByteArray();
        assertEquals(1, sink.records);
        assertEquals("the sink must receive a RECORD frame, not text",
                BinaryLogFile.FRAME_RECORD, bytes[0] & 0xFF);
        // fixed part + one entry of two 8-byte slots
        assertEquals("one entry is exactly two slots, whatever the value type",
                BinaryLogFile.RECORD_FIXED_BYTES + 16, bytes.length);
    }

    @Test
    public void aTextRecordSatisfiesTheSameContractWithItsCharacters() throws IOException {
        LogRecord text = new LogRecord(new Clock());
        text.addRecord("pricer", "price", 42);

        ForwardingSink sink = new ForwardingSink();
        sink.processLogRecord(text);

        String written = sink.captured.toString("UTF-8");
        assertTrue("a text record satisfies the byte path by exposing its characters, which is what "
                + "the spec says: " + written, written.contains("pricer"));
        assertTrue(written, written.contains("price"));
    }

    @Test
    public void theDefaultPathIsAvailableToAnySinkThatDidNotOverrideIt() throws IOException {
        // The point of a default method: a listener written before this existed still gets the path.
        LogRecordListener legacy = record -> { };
        BinaryLogRecord record = new BinaryLogRecord(new Clock());
        record.triggerObject(new TickEvent());

        OutputStream out = new ByteArrayOutputStream();
        // an existing listener must gain the byte path without being changed
        legacy.processLogRecord(record, out);
    }

    @Test
    public void theEncodedFrameMatchesWhatTheWriterProduces() throws IOException {
        // The claim that makes this useful rather than merely new: the bytes a sink takes are the same
        // bytes the shipped writer would have written for that record, so a reader can read them.
        BinaryLogRecord record = new BinaryLogRecord(new Clock());
        int node = record.internName("pricer");
        int key = record.internName("price");
        record.triggerObject(new TickEvent());
        record.addRecord(node, key, 7);

        ByteArrayOutputStream viaWriter = new ByteArrayOutputStream();
        try (BinaryLogWriter writer = new BinaryLogWriter(viaWriter)) {
            writer.processLogRecord(record);
        }
        ByteArrayOutputStream viaRecord = new ByteArrayOutputStream();
        record.encodeTo(viaRecord);

        byte[] writerBytes = viaWriter.toByteArray();
        byte[] recordBytes = viaRecord.toByteArray();
        // The writer's stream also carries the file header and dictionary frames, which are stream
        // state and not the record's - so the record's frame must appear as the TAIL of the writer's.
        assertTrue("the writer adds header and dictionary framing the record does not own",
                writerBytes.length > recordBytes.length);
        byte[] tail = new byte[recordBytes.length];
        System.arraycopy(writerBytes, writerBytes.length - recordBytes.length, tail, 0, tail.length);
        assertArrayEquals("a sink taking the record's bytes must get exactly the frame the writer "
                        + "writes, or the two paths would produce logs a reader treats differently",
                recordBytes, tail);
    }
}
