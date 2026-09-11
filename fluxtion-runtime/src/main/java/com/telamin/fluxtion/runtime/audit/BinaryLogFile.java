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

    // ---- THE BOUNDS TABLE ------------------------------------------------------------------------
    // Every field the wire writes as a u16, in one place, so both writers check the same limits and a
    // reader can state what it can represent. Three of these were unchecked in one or both writers:
    // an entry count of 65,536 wrapped to 0; a dictionary id wrapped negative; a name longer than the
    // length field emitted a short length then every byte. The C++ runtime carries this table too, in
    // fluxtion_writer.h, and the two MUST agree - a bound one side enforces and the other does not is
    // a file one side writes and the other cannot read.
    /** {@code entryCount:u16} in the record frame. */
    public static final int MAX_ENTRIES_PER_RECORD = 0xFFFF;
    /** {@code id:u16} in the dictionary frame and in every entry slot. Id 0 is reserved for "none". */
    public static final int MAX_DICTIONARY_ID = 0xFFFF;
    /** {@code len:u16} in the dictionary frame — UTF-8 bytes, not characters. */
    public static final int MAX_DICTIONARY_NAME_BYTES = 0xFFFF;

    // ---- THE TIME UNIT ---------------------------------------------------------------------------
    // The header's second u16 was reserved and written as 0. It now carries the unit of eventTime,
    // logTime and endTime, because nothing else did: Java's default clock is epoch milliseconds, the
    // C++ runtime's was epoch nanoseconds, the analyser declared every file milliseconds, and the same
    // field held both. A consumer cannot infer a unit from magnitude safely. 0 keeps its old meaning -
    // a file written before the unit was recorded - so every existing file still parses.
    /** Written by files predating the unit field. A reader may not assume a unit. */
    public static final int TIME_UNIT_UNSPECIFIED = 0;
    /** Epoch milliseconds — Java's default clock, and what the analyser assumes. */
    public static final int TIME_UNIT_EPOCH_MILLIS = 1;
    /** Epoch nanoseconds — {@code ClockStrategy.nanoEpochClock()} and the C++ {@code SystemNanoClock}. */
    public static final int TIME_UNIT_EPOCH_NANOS = 2;

    /** Fixed part of a RECORD frame: tag, entryCount, eventTypeId, and three timestamps. */
    public static final int RECORD_FIXED_BYTES = 1 + 2 + 2 + 8 + 8 + 8;

    private BinaryLogFile() {
    }
}
