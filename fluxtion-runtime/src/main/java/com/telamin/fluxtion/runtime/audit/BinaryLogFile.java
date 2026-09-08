/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

/**
 * The on-disk framing for {@link BinaryLogRecord}, shared by the writer and the reader so the two
 * cannot drift apart.
 *
 * <pre>
 *   file    := header, frame*
 *   header  := magic "FLXA" (4 bytes), formatVersion:u16, reserved:u16
 *
 *   frame   := DICT | RECORD
 *   DICT    := 0x02, id:u16, len:u16, utf8[len]
 *   RECORD  := 0x01, entryCount:u16, eventTypeId:u16,
 *              eventTime:i64, logTime:i64, endTime:i64,
 *              slots:i64[entryCount * 2]
 * </pre>
 *
 * <h2>Why the dictionary is in the file</h2>
 * Ids are assigned as names are first seen, so a reader handed only records could not say what
 * {@code nodeId 7} means. A {@code DICT} frame is written the first time an id is used, and the whole
 * dictionary is re-emitted at the start of every file so a rolled set can be read from any member
 * without replaying earlier files.
 *
 * <h2>Why a reader must tolerate truncation</h2>
 * The usual reason to read an audit log is that something went wrong, so the file is exactly as
 * complete as the process managed to make it. A partial trailing frame is a normal end state, not an
 * error, and the reader reports how many bytes it could not use rather than throwing.
 */
public final class BinaryLogFile {

    public static final byte[] MAGIC = {'F', 'L', 'X', 'A'};
    public static final int FORMAT_VERSION = 1;
    public static final int HEADER_BYTES = 8;

    public static final int FRAME_RECORD = 0x01;
    public static final int FRAME_DICT = 0x02;

    /** Fixed part of a RECORD frame: tag, entryCount, eventTypeId, and three timestamps. */
    public static final int RECORD_FIXED_BYTES = 1 + 2 + 2 + 8 + 8 + 8;

    private BinaryLogFile() {
    }
}
