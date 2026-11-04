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
 * did not strictly need it is harmless, writing one bare that did is data loss. Besides the YAML
 * rules, a value is quoted when it contains {@code ?} anywhere (SnakeYAML rejects {@code { k: ok?}}
 * and loses the whole record) or has unbalanced parentheses (the Fluxtion Audit Log Analyser's
 * tokenizer nests on them); {@code MutableOrder(id=1)} stays plain.
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
     * {@code null}; a String spelling {@code null} is quoted so the two stay distinct.
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
     * True when {@code value}, written bare inside a YAML flow map, would not read back as the same
     * String: it is empty, has leading/trailing whitespace, contains a flow indicator, comment, quote,
     * {@code ?}, control character or unpaired surrogate, has unbalanced parentheses, starts with a
     * YAML indicator, or spells a non-string scalar ({@code null}, a boolean, a number, a date).
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
        if (isIndicatorStart(first) || isEdgeSpace(first) || isEdgeSpace(value.charAt(len - 1))) {
            return true;
        }
        boolean numberLike = Character.isDigit(first);
        int parenDepth = 0;
        for (int i = 0; i < len; i++) {
            final char c = value.charAt(i);
            if (Character.isSurrogate(c)) {
                if (Character.isHighSurrogate(c) && i + 1 < len && Character.isLowSurrogate(value.charAt(i + 1))) {
                    i++;
                    numberLike = false;
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
            if (numberLike && !isNumberChar(c)) {
                numberLike = false;
            }
        }
        return parenDepth != 0 || numberLike || (len <= 8 && isReservedWord(value, len));
    }

    /** {@link #needsQuoting(CharSequence)} for a one-character value. */
    public static boolean needsQuoting(char c) {
        return isIndicatorStart(c) || isSpecial(c) || isEdgeSpace(c) || Character.isDigit(c)
                || c == '(' || c == ')'
                || c == '~' || c == 'y' || c == 'Y' || c == 'n' || c == 'N';
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
     * True when {@code value}, written bare as a block-context scalar to the end of its line, would not
     * read back as the same String: it is empty; contains a line break, control, non-printable or
     * unpaired surrogate; starts with a YAML indicator; starts or ends with whitespace (any Unicode
     * space); contains {@code ": "} or {@code " #"}; ends with {@code :}; or spells a non-string
     * scalar ({@code null}, a boolean, a number, a date).
     */
    public static boolean needsQuotingBlock(CharSequence value) {
        final int len = value.length();
        if (len == 0) {
            return true;
        }
        final char first = value.charAt(0);
        if (len == 1) {
            return needsQuoting(first);
        }
        final char last = value.charAt(len - 1);
        if (isIndicatorStart(first) || isEdgeSpace(first) || isEdgeSpace(last) || last == ':') {
            return true;
        }
        boolean numberLike = Character.isDigit(first);
        for (int i = 0; i < len; i++) {
            final char c = value.charAt(i);
            if (Character.isSurrogate(c)) {
                if (Character.isHighSurrogate(c) && i + 1 < len && Character.isLowSurrogate(value.charAt(i + 1))) {
                    i++;
                    numberLike = false;
                    continue;
                }
                return true;
            }
            if (c < ' ' || isNonPrintable(c)
                    || (c == ':' && value.charAt(i + 1) == ' ')       // i + 1 < len: last is not ':'
                    || (c == '#' && value.charAt(i - 1) == ' ')) {    // i > 0: first is not '#'
                return true;
            }
            if (numberLike && !isNumberChar(c)) {
                numberLike = false;
            }
        }
        return numberLike || (len <= 8 && isReservedWord(value, len));
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
            case '%': case '@': case '`': case '+': case '.': case '=': case '<': case '~':
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

    /**
     * Characters that can make up a number, date or sexagesimal value starting with a digit
     * (YAML 1.1 int/float/timestamp, Java hex/float spellings). A digit-led value made only of these is
     * quoted; anything else, such as {@code 30 days}, stays plain. Any Unicode digit counts: a
     * reader typing numbers with {@code Long.parseLong} accepts Arabic-Indic or fullwidth
     * digits as well as ASCII ones.
     */
    private static boolean isNumberChar(char c) {
        return Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
                || c == 'x' || c == 'X' || c == 'o' || c == 'O' || c == 'p' || c == 'P'
                || c == '_' || c == '.' || c == '+' || c == '-'
                // sexagesimal and timestamps (12:30:00, 2001-12-14 21:59:43.10 -5, ...T...Z): a ':' is
                // already special in flow context, but legal plain in a block header field
                || c == ':' || c == ' ' || c == 'T' || c == 't' || c == 'Z' || c == 'z';
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
