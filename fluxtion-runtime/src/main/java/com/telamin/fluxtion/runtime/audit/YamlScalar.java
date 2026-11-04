/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

/**
 * Writes a string-valued audit entry as a YAML scalar that reads back as the same string.
 *
 * <p>Text audit values sit inside a YAML flow map ({@code - node: { key: value}}). Written bare, a
 * value containing a flow indicator breaks the record's structure: the String
 * {@code membership not active, balance due 30.0} came back as {@code membership not active} plus a
 * second, invented entry. Such a value is written in YAML double-quoted style; every other value is
 * written plain, exactly as runtime 1.1.3 wrote it.
 *
 * <p><b>Only structurally unsafe text is quoted.</b> Typed-looking strings - {@code 5000},
 * {@code 1e3}, {@code 0x1F}, {@code true}, {@code null}, {@code ~}, a date - are written bare and read
 * with YAML/Analyser typing, as before: a String {@code "5000"} has always read back as the number
 * 5000, the Analyser graphs only bare numbers, and exports depend on that typing. The empty string
 * is also written bare, as 1.1.3 did (it reads as null); it cannot break the structure.
 *
 * <p>The check is a single pass of {@code charAt} with no allocation and no regex — this runs on the
 * audit hot path for every string-valued entry. Besides the YAML flow indicators, a value is quoted
 * when it contains {@code ?} anywhere (SnakeYAML rejects {@code { k: ok?}} and loses the whole record)
 * or has unbalanced parentheses (the Analyser's tokenizer nests on them); {@code MutableOrder(id=1)}
 * stays plain.
 *
 * <h2>Escapes</h2>
 * Inside a double-quoted scalar exactly these escapes are emitted, all standard YAML 1.1/1.2:
 * <ul>
 * <li>{@code \\} backslash, {@code \x22} double quote, {@code \n}, {@code \r}, {@code \t}, {@code \0}
 * <li>{@code \}{@code uXXXX} (lowercase hex, BMP) for every other non-printable character: C0
 * controls, DEL and C1 controls, U+2028, U+2029, U+FEFF, U+FFFE, U+FFFF
 * </ul>
 * The double quote is written {@code \x22}, never {@code \"}: the analyser's legacy text scanner reads
 * {@code \"} as a closing quote, and would lose the entry boundary. A valid surrogate pair (a
 * supplementary character) is written as-is, unescaped. An <b>unpaired</b> surrogate cannot be
 * encoded in UTF-8 at all, so it forces quoting and is written as {@code \}{@code ufffd} (U+FFFD,
 * the replacement character) — lossy by necessity, but visible rather than a silent {@code ?}.
 */
public final class YamlScalar {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private YamlScalar() {
    }

    /**
     * Appends {@code value} as a flow-context YAML scalar. A {@code null} is written as the YAML null
     * {@code null}. A String spelling {@code null}, {@code true} or a number is written bare too, and
     * reads with YAML typing, as it did before this class existed.
     *
     * @return true when the value was written in double-quoted form
     */
    public static boolean append(StringBuilder sb, CharSequence value) {
        if (value == null) {
            sb.append("null");
            return false;
        } else if (needsQuoting(value)) {
            appendQuoted(sb, value);
            return true;
        } else {
            sb.append(value);
            return false;
        }
    }

    /**
     * Appends a single character as a flow-context YAML scalar that reads back as a one-char string.
     *
     * @return true when the value was written in double-quoted form
     */
    public static boolean append(StringBuilder sb, char value) {
        if (needsQuoting(value)) {
            sb.append('"');
            appendEscaped(sb, value);
            sb.append('"');
            return true;
        } else {
            sb.append(value);
            return false;
        }
    }

    /**
     * True when {@code value}, written bare inside a YAML flow map, would break the record's structure:
     * it has leading/trailing whitespace (any Unicode space), contains a flow indicator, comment,
     * quote, {@code ?}, control character or unpaired surrogate, has unbalanced parentheses, or starts
     * with a YAML indicator. A value that merely looks like a number, boolean or null is NOT quoted.
     */
    public static boolean needsQuoting(CharSequence value) {
        final int len = value.length();
        if (len == 0) {
            return false;   // bare, as 1.1.3 wrote it
        }
        final char first = value.charAt(0);
        if (len == 1) {
            return needsQuoting(first);
        }
        if (startsWithIndicator(value) || isEdgeSpace(first) || isEdgeSpace(value.charAt(len - 1))) {
            return true;
        }
        int parenDepth = 0;
        for (int i = 0; i < len; i++) {
            final char c = value.charAt(i);
            if (Character.isSurrogate(c)) {
                if (Character.isHighSurrogate(c) && i + 1 < len && Character.isLowSurrogate(value.charAt(i + 1))) {
                    i++;
                    continue;
                }
                return true;
            }
            if (isSpecial(c)) {
                return true;
            }
            if (c == '(') {
                parenDepth++;
            } else if (c == ')' && --parenDepth < 0) {
                return true;
            }
        }
        return parenDepth != 0;
    }

    /** {@link #needsQuoting(CharSequence)} for a one-character value. */
    public static boolean needsQuoting(char c) {
        return isIndicatorStart(c) || isSpecial(c) || isEdgeSpace(c) || c == '(' || c == ')';
    }

    /**
     * Appends {@code value} as a <b>block-context</b> YAML scalar - a record header field such as
     * {@code eventToString: ...}, which runs to the end of its line. Same quoting and escapes as
     * {@link #append(StringBuilder, CharSequence)}, with the block rule {@link #needsQuotingBlock}:
     * commas, brackets, braces and parentheses mid-value are legal plain there, so a typical
     * {@code toString()} such as {@code Trade[id=1, px=2.0]} or {@code X(a=1, b=2)} stays plain.
     *
     * @return true when the value was written in double-quoted form
     */
    public static boolean appendBlock(StringBuilder sb, CharSequence value) {
        if (value == null) {
            sb.append("null");
            return false;
        } else if (needsQuotingBlock(value)) {
            appendQuoted(sb, value);
            return true;
        } else {
            sb.append(value);
            return false;
        }
    }

    /**
     * True when {@code value}, written bare as a block-context scalar to the end of its line, would
     * break the record's structure: it contains a line break, control, non-printable or unpaired
     * surrogate; starts with a YAML indicator; starts or ends with whitespace (any Unicode space);
     * contains {@code ": "} or {@code " #"}; or ends with {@code :}. A value that merely looks like a
     * number, boolean, null or timestamp is NOT quoted.
     */
    public static boolean needsQuotingBlock(CharSequence value) {
        final int len = value.length();
        if (len == 0) {
            return false;   // bare, as 1.1.3 wrote it
        }
        final char first = value.charAt(0);
        if (len == 1) {
            return needsQuoting(first);
        }
        final char last = value.charAt(len - 1);
        if (startsWithIndicator(value) || isEdgeSpace(first) || isEdgeSpace(last) || last == ':') {
            return true;
        }
        for (int i = 0; i < len; i++) {
            final char c = value.charAt(i);
            if (Character.isSurrogate(c)) {
                if (Character.isHighSurrogate(c) && i + 1 < len && Character.isLowSurrogate(value.charAt(i + 1))) {
                    i++;
                    continue;
                }
                return true;
            }
            if (c < ' ' || isNonPrintable(c)
                    || (c == ':' && value.charAt(i + 1) == ' ')       // i + 1 < len: last is not ':'
                    || (c == '#' && value.charAt(i - 1) == ' ')) {    // i > 0: first is not '#'
                return true;
            }
        }
        return false;
    }

    /**
     * A value of two or more characters starting with a YAML indicator. {@code -} is one only when a
     * space follows it ({@code - item}); {@code -5} and {@code -dash} are ordinary plain scalars.
     */
    private static boolean startsWithIndicator(CharSequence value) {
        final char first = value.charAt(0);
        if (first == '-') {
            return isEdgeSpace(value.charAt(1));
        }
        return isIndicatorStart(first);
    }

    /**
     * Whitespace a YAML reader strips from the ends of a plain scalar - or that a reader may: any
     * {@link Character#isWhitespace} or {@link Character#isSpaceChar} character, so U+00A0, U+2003 and
     * U+3000 count as well as the ASCII space.
     */
    private static boolean isEdgeSpace(char c) {
        return c == ' ' || Character.isWhitespace(c) || Character.isSpaceChar(c);
    }

    /** Appends {@code value} in YAML double-quoted style, escaping as described on this class. */
    public static void appendQuoted(StringBuilder sb, CharSequence value) {
        sb.append('"');
        for (int i = 0, len = value.length(); i < len; i++) {
            final char c = value.charAt(i);
            if (Character.isHighSurrogate(c) && i + 1 < len && Character.isLowSurrogate(value.charAt(i + 1))) {
                // a valid supplementary character: written as-is
                sb.append(c).append(value.charAt(++i));
            } else {
                appendEscaped(sb, c);
            }
        }
        sb.append('"');
    }

    private static void appendEscaped(StringBuilder sb, char c) {
        switch (c) {
            case '"': sb.append("\\x22"); return;
            case '\\': sb.append("\\\\"); return;
            case '\n': sb.append("\\n"); return;
            case '\r': sb.append("\\r"); return;
            case '\t': sb.append("\\t"); return;
            case '\0': sb.append("\\0"); return;
            default:
                if (Character.isSurrogate(c)) {
                    // unpaired: not encodable, written as the replacement character
                    sb.append("\\ufffd");
                } else if (isNonPrintable(c)) {
                    sb.append("\\u")
                            .append(HEX[(c >>> 12) & 0xF]).append(HEX[(c >>> 8) & 0xF])
                            .append(HEX[(c >>> 4) & 0xF]).append(HEX[c & 0xF]);
                } else {
                    sb.append(c);
                }
        }
    }

    /** A character that cannot appear in a plain flow scalar, or that a reader would strip or split on. */
    private static boolean isSpecial(char c) {
        switch (c) {
            case ',': case ':': case '{': case '}': case '[': case ']':
            case '#': case '"': case '\'': case '?':
                return true;
            default:
                return c < ' ' || isNonPrintable(c) || Character.isSurrogate(c);
        }
    }

    /** A YAML indicator that changes the meaning of a scalar when it is the first character. */
    private static boolean isIndicatorStart(char c) {
        switch (c) {
            case '-': case '?': case ':': case ',': case '[': case ']': case '{': case '}':
            case '#': case '&': case '*': case '!': case '|': case '>': case '\'': case '"':
            case '%': case '@': case '`': case '=': case '<':
                return true;
            default:
                return false;
        }
    }

    private static boolean isNonPrintable(char c) {
        return (c < ' ' && c != '\t') || (c >= 0x7F && c <= 0x9F)
                || c == 0x2028 || c == 0x2029   // line and paragraph separators
                || c == 0xFEFF || c == 0xFFFE || c == 0xFFFF;   // BOM and the two noncharacters
    }
}
