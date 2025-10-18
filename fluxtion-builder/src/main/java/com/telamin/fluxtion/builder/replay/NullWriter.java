/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.replay;

import java.io.Writer;

public class NullWriter extends Writer {

    /**
     * A singleton.
     */
    public static final NullWriter NULL_WRITER = new NullWriter();

    /**
     * Constructs a new NullWriter.
     */
    public NullWriter() {
    }

    /**
     * Does nothing - output to {@code /dev/null}.
     *
     * @param c The character to write
     * @return this writer
     * @since 2.0
     */
    @Override
    public Writer append(final char c) {
        //to /dev/null
        return this;
    }

    /**
     * Does nothing - output to {@code /dev/null}.
     *
     * @param csq   The character sequence to write
     * @param start The index of the first character to write
     * @param end   The index of the first character to write (exclusive)
     * @return this writer
     * @since 2.0
     */
    @Override
    public Writer append(final CharSequence csq, final int start, final int end) {
        //to /dev/null
        return this;
    }

    /**
     * Does nothing - output to {@code /dev/null}.
     *
     * @param csq The character sequence to write
     * @return this writer
     * @since 2.0
     */
    @Override
    public Writer append(final CharSequence csq) {
        //to /dev/null
        return this;
    }

    /**
     * Does nothing - output to {@code /dev/null}.
     *
     * @param idx The character to write
     */
    @Override
    public void write(final int idx) {
        //to /dev/null
    }

    /**
     * Does nothing - output to {@code /dev/null}.
     *
     * @param chr The characters to write
     */
    @Override
    public void write(final char[] chr) {
        //to /dev/null
    }

    /**
     * Does nothing - output to {@code /dev/null}.
     *
     * @param chr The characters to write
     * @param st  The start offset
     * @param end The number of characters to write
     */
    @Override
    public void write(final char[] chr, final int st, final int end) {
        //to /dev/null
    }

    /**
     * Does nothing - output to {@code /dev/null}.
     *
     * @param str The string to write
     */
    @Override
    public void write(final String str) {
        //to /dev/null
    }

    /**
     * Does nothing - output to {@code /dev/null}.
     *
     * @param str The string to write
     * @param st  The start offset
     * @param end The number of characters to write
     */
    @Override
    public void write(final String str, final int st, final int end) {
        //to /dev/null
    }

    /**
     * @see Writer#flush()
     */
    @Override
    public void flush() {
        //to /dev/null
    }

    /**
     * @see Writer#close()
     */
    @Override
    public void close() {
        //to /dev/null
    }

}

