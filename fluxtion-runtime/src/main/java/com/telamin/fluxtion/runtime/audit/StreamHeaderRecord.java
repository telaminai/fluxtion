/*
 * Copyright: © 2026. Gregory Higgins
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

/**
 * Carries an {@link AuditStreamHeader} to a sink as a {@link LogRecord}, so every existing sink writes it with no
 * change: a text sink prints {@link #asCharSequence()} as it prints any record, and {@link BinaryLogWriter}
 * recognises the type and writes a {@link BinaryLogFile#FRAME_STREAM_HEADER} frame.
 *
 * <p>A sink that counts or filters records should test {@code record instanceof StreamHeaderRecord}: the header
 * is metadata about the stream, not an event cycle.
 */
public final class StreamHeaderRecord extends LogRecord {

    private final AuditStreamHeader header;
    private final String text;
    private final boolean binaryStream;

    public StreamHeaderRecord(AuditStreamHeader header) {
        this(header, false);
    }

    /**
     * @param binaryStream true when the stream this header opens carries {@link BinaryLogRecord}s; it decides
     *                     whether {@link LogRecordListener#processStreamHeader} delivers it as a record by default
     */
    public StreamHeaderRecord(AuditStreamHeader header, boolean binaryStream) {
        super(null);
        this.header = header;
        this.binaryStream = binaryStream;
        this.text = header.toString();
        sb.append(text);
    }

    public AuditStreamHeader header() {
        return header;
    }

    public boolean isBinaryStream() {
        return binaryStream;
    }

    @Override
    public CharSequence asCharSequence() {
        return text;
    }

    /** The header is immutable: clearing it after publish, as a sink may, must not empty it. */
    @Override
    public void clear() {
    }

    @Override
    public boolean terminateRecord() {
        return true;
    }
}
