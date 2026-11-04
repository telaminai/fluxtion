/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

/**
 * Writes a string-valued audit entry as a YAML scalar that reads back as the same string.
 *
 * <p>Text audit values sit inside a YAML flow map ({@code - node: { key: value}}). Written bare, a
 * value containing a flow indicator is cut short or re-typed by any YAML reader: the String
 * {@code membership not active, balance due 30.0} came back as {@code membership not active}, and the
 * String {@code 123} came back as an integer. A value that would be misread is written in YAML
 * double-quoted style; every other value is written plain, exactly as before, so existing logs and
 * diffs of simple values do not move.
 *
 * <p>The check is a single pass of {@code charAt} with no allocation and no regex — this runs on the
 * audit hot path for every string-valued entry. It is deliberately conservative: quoting a value that
 * did not strictly need it is harmless, writing one bare that did is data loss.
 */
public final class YamlScalar {

    private YamlScalar() {
    }

    /**
     * Appends {@code value} as a flow-context YAML scalar. A {@code null} is written as the YAML null
     * {@code null}; a String spelling {@code null} is quoted so the two stay distinct.
     */
    public static void append(StringBuilder sb, CharSequence value) {
        if (value == null) {
            sb.append("null");
        } else if (needsQuoting(value)) {
            appendQuoted(sb, value);
        } else {
            sb.append(value);
        }
    }

    /** Appends a single character as a flow-context YAML scalar that reads back as a one-char string. */
    public static void append(StringBuilder sb, char value) {
        if (needsQuoting(value)) {
            sb.append('"');
            appendEscaped(sb, value);
            sb.append('"');
        } else {
            sb.append(value);
        }
    }

    /**
     * True when {@code value}, written bare inside a YAML flow map, would not read back as the same
     * String: it is empty, has leading/trailing whitespace, contains a flow indicator, comment, quote
     * or control character, starts with a YAML indicator, or spells a non-string scalar
     * ({@code null}, a boolean, a number, a date).
     */
    public static boolean needsQuoting(CharSequence value) {
        final int len = value.length();
        if (len == 0) {
            return true;
        }
        final char first = value.charAt(0);
        if (len == 1) {
            return needsQuoting(first);
        }
        if (isIndicatorStart(first) || first == ' ' || value.charAt(len - 1) == ' ') {
            return true;
        }
        boolean numberLike = first >= '0' && first <= '9';
        for (int i = 0; i < len; i++) {
            final char c = value.charAt(i);
            if (isSpecial(c)) {
                return true;
            }
            if (numberLike && !isNumberChar(c)) {
                numberLike = false;
            }
        }
        return numberLike || (len <= 8 && isReservedWord(value, len));
    }

    /** {@link #needsQuoting(CharSequence)} for a one-character value. */
    public static boolean needsQuoting(char c) {
        return isIndicatorStart(c) || isSpecial(c) || c == ' ' || (c >= '0' && c <= '9')
                || c == '~' || c == 'y' || c == 'Y' || c == 'n' || c == 'N';
    }

    /** Appends {@code value} in YAML double-quoted style, escaping as the YAML spec requires. */
    public static void appendQuoted(StringBuilder sb, CharSequence value) {
        sb.append('"');
        for (int i = 0, len = value.length(); i < len; i++) {
            appendEscaped(sb, value.charAt(i));
        }
        sb.append('"');
    }

    private static void appendEscaped(StringBuilder sb, char c) {
        switch (c) {
            case '"': sb.append("\\\""); return;
            case '\\': sb.append("\\\\"); return;
            case '\n': sb.append("\\n"); return;
            case '\r': sb.append("\\r"); return;
            case '\t': sb.append("\\t"); return;
            case '\0': sb.append("\\0"); return;
            default:
                if (isNonPrintable(c)) {
                    sb.append("\\u");
                    String hex = Integer.toHexString(c);
                    for (int pad = hex.length(); pad < 4; pad++) {
                        sb.append('0');
                    }
                    sb.append(hex);
                } else {
                    sb.append(c);
                }
        }
    }

    /** A character that cannot appear in a plain flow scalar, or that a reader would strip or split on. */
    private static boolean isSpecial(char c) {
        switch (c) {
            case ',': case ':': case '{': case '}': case '[': case ']':
            case '#': case '"': case '\'':
                return true;
            default:
                return c < ' ' || isNonPrintable(c);
        }
    }

    /** A YAML indicator that changes the meaning of a scalar when it is the first character. */
    private static boolean isIndicatorStart(char c) {
        switch (c) {
            case '-': case '?': case ':': case ',': case '[': case ']': case '{': case '}':
            case '#': case '&': case '*': case '!': case '|': case '>': case '\'': case '"':
            case '%': case '@': case '`': case '+': case '.': case '=': case '<': case '~':
                return true;
            default:
                return false;
        }
    }

    private static boolean isNonPrintable(char c) {
        return (c < ' ' && c != '\t') || (c >= 0x7F && c <= 0x9F) || c == ' ' || c == ' '
                || c == '﻿' || c == '￾' || c == '￿';
    }

    /**
     * Characters that can make up a number, date or sexagesimal value starting with a digit
     * (YAML 1.1 int/float/timestamp, Java hex/float spellings). A digit-led value made only of these is
     * quoted; anything else, such as {@code 30 days}, stays plain.
     */
    private static boolean isNumberChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
                || c == 'x' || c == 'X' || c == 'o' || c == 'O' || c == 'p' || c == 'P'
                || c == '_' || c == '.' || c == '+' || c == '-';
    }

    /** YAML 1.1 booleans and null, plus the spellings an audit reader types as numbers. */
    private static boolean isReservedWord(CharSequence v, int len) {
        switch (Character.toLowerCase(v.charAt(0))) {
            case 'y': return len == 3 && eqIgnoreCase(v, "yes");
            case 'n': return (len == 2 && eqIgnoreCase(v, "no")) || (len == 4 && eqIgnoreCase(v, "null"))
                    || (len == 3 && eqIgnoreCase(v, "nan"));
            case 't': return len == 4 && eqIgnoreCase(v, "true");
            case 'f': return len == 5 && eqIgnoreCase(v, "false");
            case 'o': return (len == 2 && eqIgnoreCase(v, "on")) || (len == 3 && eqIgnoreCase(v, "off"));
            case 'i': return len == 8 && eqIgnoreCase(v, "infinity");
            default: return false;
        }
    }

    private static boolean eqIgnoreCase(CharSequence v, String word) {
        for (int i = 0; i < word.length(); i++) {
            if (Character.toLowerCase(v.charAt(i)) != word.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
