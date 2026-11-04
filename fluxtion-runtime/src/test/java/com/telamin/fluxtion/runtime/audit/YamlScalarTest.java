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
        assertEquals("\"say \\\"hi\\\"\"", w("say \"hi\""));
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
        assertEquals("\"\\\"\"", w('"'));
    }
}
