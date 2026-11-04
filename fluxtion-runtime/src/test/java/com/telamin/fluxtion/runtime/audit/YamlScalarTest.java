/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The exact text {@link YamlScalar} writes. Plain values must not move (existing logs and diffs stay
 * stable); values a YAML reader would misread are double-quoted with YAML escapes. The round trip
 * through a real YAML parser is {@code AuditValueYamlRoundTripTest} in fluxtion-builder-api, which has
 * SnakeYAML on its classpath.
 */
public class YamlScalarTest {

    private static String w(CharSequence v) {
        StringBuilder sb = new StringBuilder();
        YamlScalar.append(sb, v);
        return sb.toString();
    }

    private static String w(char v) {
        StringBuilder sb = new StringBuilder();
        YamlScalar.append(sb, v);
        return sb.toString();
    }

    @Test
    public void simpleValuesArePlain() {
        for (String v : new String[]{"simple", "membership active", "Tick->getPrice", "30 days",
                "MutableOrder(id=1)", "a-b_c.d", "back\\slash", "abc123", "x=1"}) {
            assertEquals(v, w(v));
        }
        assertEquals("a", w('a'));
        assertEquals("null", w((CharSequence) null));
    }

    @Test
    public void ambiguousValuesAreDoubleQuotedAndEscaped() {
        assertEquals("\"membership not active, balance due 30.0\"", w("membership not active, balance due 30.0"));
        assertEquals("\"k: v\"", w("k: v"));
        assertEquals("\"say \\x22hi\\x22\"", w("say \"hi\""));
        assertEquals("\"it's\"", w("it's"));
        assertEquals("\"a\\nb\\tc\\r\\\\\"", w("a\nb\tc\r\\"));
        assertEquals("\"\\u0001\"", w("\u0001"));
        assertEquals("\"\"", w(""));
        assertEquals("\" pad \"", w(" pad "));
        assertEquals("\"null\"", w("null"));
        assertEquals("\"true\"", w("true"));
        assertEquals("\"123\"", w("123"));
        assertEquals("\"-5\"", w("-5"));
        assertEquals("\"#x\"", w("#x"));
        assertEquals("\",\"", w(','));
        assertEquals("\"7\"", w('7'));
        assertEquals("\"\\x22\"", w('"'));
    }

    @Test
    public void questionMarkAnywhereIsQuoted() {
        assertEquals("\"ok?\"", w("ok?"));
        assertEquals("\"is it ok?\"", w("is it ok?"));
        assertEquals("\"url?x=1\"", w("url?x=1"));
        assertEquals("\"?\"", w('?'));
    }

    @Test
    public void unbalancedParenthesesAreQuoted() {
        assertEquals("MutableOrder(id=1)", w("MutableOrder(id=1)"));
        assertEquals("f(g(x))", w("f(g(x))"));
        assertEquals("\"open(\"", w("open("));
        assertEquals("\"close)\"", w("close)"));
        assertEquals("\"a)b(c\"", w("a)b(c"));
        assertEquals("\"f(g(x)\"", w("f(g(x)"));
        assertEquals("\"(\"", w('('));
        assertEquals("\")\"", w(')'));
    }

    @Test
    public void surrogates() {
        String pair = "smile \uD83D\uDE00";
        assertEquals("a valid pair needs no quoting", pair, w(pair));
        assertEquals("written raw inside a quoted value", "\"a, \uD83D\uDE00\"", w("a, \uD83D\uDE00"));
        assertEquals("\"lone \\ufffd x\"", w("lone \uD800 x"));
        assertEquals("\"low \\ufffd\"", w("low \uDC00"));
        assertEquals("\"\\ufffd\\ufffd\"", w("\uDC00\uD800"));
        assertEquals("\"\\ufffd\"", w('\uD800'));
    }

    @Test
    public void escapeSetIsExactly() {
        assertEquals("\"\\\\ \\x22 \\n \\r \\t \\0 \\u0001 \\u007f \\u0085 \\u2028 \\u2029 \\ufeff \\ufffe \\uffff\"",
                w("\\ \" \n \r \t \0 \u0001 \u007f \u0085 \u2028 \u2029 \uFEFF \uFFFE \uFFFF"));
        assertEquals("no \\\" escape is ever written", -1, w("a\"b, c").indexOf("\\\""));
    }

    @Test
    public void appendReportsWhetherItQuoted() {
        StringBuilder sb = new StringBuilder();
        assertEquals(false, YamlScalar.append(sb, "plain"));
        assertEquals(false, YamlScalar.append(sb, (CharSequence) null));
        assertEquals(true, YamlScalar.append(sb, "a, b"));
        assertEquals(false, YamlScalar.append(sb, 'a'));
        assertEquals(true, YamlScalar.append(sb, ','));
    }
}
