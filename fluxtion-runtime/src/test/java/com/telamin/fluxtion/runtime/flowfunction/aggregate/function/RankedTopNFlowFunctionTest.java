/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** Unit coverage for {@link RankedTopNFlowFunction} — ordered bounded insert, combine merge, reset. */
public class RankedTopNFlowFunctionTest {

    static final class Row {
        private final String grp;
        private final int score;
        Row(String grp, int score) { this.grp = grp; this.score = score; }
        String grp() { return grp; }
        int score() { return score; }
    }

    private static RankedTopNFlowFunction<Row> topN(int n) {
        // newRanked() builds a fresh aggregate carrying the ranking accessor + bound.
        return new RankedTopNFlowFunction.RankedTopNFactory<Row>(Row::score, n).newRanked();
    }

    private static List<Integer> scores(List<Row> rows) {
        return rows.stream().map(Row::score).collect(Collectors.toList());
    }

    @Test
    public void keepsTopNDescendingAndBounded() {
        RankedTopNFlowFunction<Row> agg = topN(2);
        agg.aggregate(new Row("A", 10));
        agg.aggregate(new Row("A", 30));
        agg.aggregate(new Row("A", 20));
        agg.aggregate(new Row("A", 25));
        assertEquals("top-2 by score desc, bounded", Arrays.asList(30, 25), scores(agg.get()));
    }

    @Test
    public void fewerThanNReturnsAllDescending() {
        RankedTopNFlowFunction<Row> agg = topN(5);
        agg.aggregate(new Row("A", 4));
        agg.aggregate(new Row("A", 9));
        assertEquals(Arrays.asList(9, 4), scores(agg.get()));
    }

    @Test
    public void tiesKeepArrivalOrder() {
        RankedTopNFlowFunction<Row> agg = topN(3);
        Row first = new Row("A", 5);
        Row second = new Row("A", 5);
        agg.aggregate(first);
        agg.aggregate(second);
        // both score 5 -> arrival order preserved (first inserted stays ahead).
        assertEquals(Arrays.asList(first, second), agg.get());
    }

    @Test
    public void combineMergesToGlobalTopN() {
        RankedTopNFlowFunction<Row> a = topN(2);
        a.aggregate(new Row("A", 10));
        a.aggregate(new Row("A", 1));
        RankedTopNFlowFunction<Row> b = topN(2);
        b.aggregate(new Row("A", 9));
        b.aggregate(new Row("A", 8));
        a.combine(b);
        assertEquals("union of per-bucket top-N contains global top-N", Arrays.asList(10, 9), scores(a.get()));
    }

    @Test
    public void resetClears() {
        RankedTopNFlowFunction<Row> agg = topN(2);
        agg.aggregate(new Row("A", 10));
        agg.reset();
        assertEquals(Collections.emptyList(), agg.get());
        assertFalse("ordered bounded insert/trim is not invertible", agg.deductSupported());
    }
}
