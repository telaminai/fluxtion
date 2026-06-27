/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function;

import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive.DoubleMaxFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive.DoubleMinFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive.IntMaxFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupByFlowFunctionWrapper;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Set;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Failing tests that pin the sliding-window deduct/invertibility bugs.
 *
 * <p>The full analysis, fix plan and failure report are kept in the internal
 * windowing-deduct-invertibility regression notes. These tests exercise the runtime classes
 * directly (no clock / no generated graph) so the failure is deterministic and the size/shape is
 * unambiguous. They are expected to FAIL against the current runtime and to PASS once the proposed
 * fixes land.
 *
 * <p>Algebraic principle under test: {@code combine}/{@code deduct} is exact incremental window
 * maintenance ONLY for invertible (group) aggregates — sum, count, average. For non-invertible
 * (semilattice) aggregates — min, max, distinct-set — there is no inverse, so the engine must take
 * the full-recompute branch in {@link BucketedSlidingWindow#roll(int)}. The bugs below are all
 * cases where a non-invertible aggregate is wrongly routed onto the deduct path.
 *
 * <p><b>@Ignore</b>: these reproduce OPEN bugs and fail/error today (3 failures, 1 error). They are
 * committed disabled so the module build stays green; remove the {@code @Ignore} to verify a fix —
 * all four must then pass with no edit to the assertions.
 */
@Ignore("Reproduces open sliding-window deduct/invertibility bugs (see internal regression notes). "
        + "Remove @Ignore to verify the fix.")
public class WindowingDeductInvertibilityTest {

    /** Minimal input: a symbol-keyed price. Plain class (not a record) to keep the source level low. */
    static final class Quote {
        final String sym;
        final int px;

        Quote(String sym, int px) {
            this.sym = sym;
            this.px = px;
        }

        String sym() {
            return sym;
        }

        int px() {
            return px;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // BUG 1 — grouped sliding MAX is mis-routed onto the deduct path and throws at roll.
    // ---------------------------------------------------------------------------------------------

    /**
     * Root cause, pinned in isolation: a {@link GroupByFlowFunctionWrapper} around a non-invertible
     * inner aggregate ({@link IntMaxFlowFunction}, whose {@code deductSupported()==false}) must itself
     * report {@code deductSupported()==false}. Today the wrapper does not override the method, inherits
     * the interface default {@code true}, and so {@link BucketedSlidingWindow} samples {@code true} and
     * picks the deduct branch for a grouped max.
     *
     * <p>Failure shape: {@code assertFalse} sees {@code true}.
     */
    @Test
    public void groupedWrapperDeductSupportedMustDelegateToInnerAggregate() {
        GroupByFlowFunctionWrapper<Quote, String, Integer, Integer, IntMaxFlowFunction> wrapper =
                new GroupByFlowFunctionWrapper<>(Quote::sym, Quote::px, IntMaxFlowFunction::new);

        assertFalse(
                "grouped wrapper must delegate deductSupported() to its inner aggregate "
                        + "(IntMax is non-invertible -> false); inheriting the interface default true "
                        + "routes grouped max onto the deduct path",
                wrapper.deductSupported());
    }

    /**
     * End-to-end consequence: a 2-bucket sliding window of {@code group by sym -> max(px)} where one
     * key spans both buckets. When the oldest bucket expires, the deduct branch calls
     * {@code IntMaxFlowFunction.deduct(...)}, which is unimplemented and throws the interface default
     * {@code RuntimeException("Sliding not supported implement deduct for ...IntMaxFlowFunction")}.
     *
     * <p>Correct (post-fix, recompute branch): the window holds the two live buckets — AAA in
     * {3} and {9} after the roll — so {@code max == 9}.
     *
     * <p>Failure shape: throws {@link RuntimeException} during the third {@code roll()} (data-dependent
     * — only fires when a key is present in >=2 live buckets, so it survives sparse-key smoke tests).
     */
    @Test
    public void groupedSlidingMaxKeySpanningBucketsThrowsOnExpiry() {
        SerializableFunction<Quote, String> keyFn = Quote::sym;
        SerializableFunction<Quote, Integer> valFn = Quote::px;
        SerializableSupplier<IntMaxFlowFunction> aggFn = IntMaxFlowFunction::new;
        Supplier<GroupByFlowFunctionWrapper<Quote, String, Integer, Integer, IntMaxFlowFunction>> supplier =
                () -> new GroupByFlowFunctionWrapper<>(keyFn, valFn, aggFn);

        BucketedSlidingWindow<Quote, GroupBy<String, Integer>,
                GroupByFlowFunctionWrapper<Quote, String, Integer, Integer, IntMaxFlowFunction>> window =
                new BucketedSlidingWindow<>(supplier, 2);

        window.aggregate(new Quote("AAA", 5));
        window.roll();                              // bucket0 = {AAA:5}
        window.aggregate(new Quote("AAA", 9));
        window.roll();                              // bucket1 = {AAA:9}; window full
        window.aggregate(new Quote("AAA", 3));
        window.roll();                              // bucket0 (AAA:5) expires -> deduct -> THROWS today

        GroupBy<String, Integer> result = window.get();
        assertEquals("sliding max over the two live buckets {3, 9}",
                Integer.valueOf(9), result.toMap().get("AAA"));
    }

    // ---------------------------------------------------------------------------------------------
    // BUG 2 — sliding distinct-set silently drops a value still live in another bucket.
    // ---------------------------------------------------------------------------------------------

    /**
     * {@link AggregateToSetFlowFunction} implements {@code deduct} as {@code Set.removeAll(...)} and
     * does not override {@code deductSupported()} (defaults true), so a sliding window takes the deduct
     * branch. {@code removeAll} is not multiplicity-aware: when the expiring bucket and a still-live
     * bucket both contain a value, expiry removes it from the whole-window set even though it is still
     * present. Silent wrong answer (no exception).
     *
     * <p>Sequence (2 buckets): value 7 lands in bucket0 and bucket1, then bucket0 (also holding 7)
     * expires while bucket1 still holds 7. Correct window set = {7, 8}. Buggy result = {8}.
     *
     * <p>Failure shape: {@code expected {7, 8}} but {@code actual {8}} — silently dropped element 7.
     */
    @Test
    public void slidingDistinctSetDropsValueStillLiveInAnotherBucket() {
        Supplier<AggregateToSetFlowFunction<Integer>> supplier = AggregateToSetFlowFunction::new;
        BucketedSlidingWindow<Integer, Set<Integer>, AggregateToSetFlowFunction<Integer>> window =
                new BucketedSlidingWindow<>(supplier, 2);

        window.aggregate(7);
        window.roll();      // bucket0 = {7}
        window.aggregate(7);
        window.roll();      // bucket1 = {7}; window full
        window.aggregate(8);
        window.roll();      // bucket0 ({7}) expires; removeAll({7}) wrongly strips the still-live 7

        Set<Integer> result = window.get();
        assertEquals("both live buckets' values must remain — bucket1 still holds 7",
                Set.of(7, 8), result);
    }

    // ---------------------------------------------------------------------------------------------
    // BUG 3 — DoubleMin is poisoned by a NaN input; DoubleMax (which guards NaN) is not.
    // ---------------------------------------------------------------------------------------------

    /**
     * {@link DoubleMaxFlowFunction#aggregateDouble(double)} guards {@code if (!Double.isNaN(input))};
     * {@link DoubleMinFlowFunction#aggregateDouble(double)} does not. {@code Math.min(5.0, NaN)} is NaN,
     * so a single NaN input pins min to NaN until reset, while max shrugs it off. Asymmetric.
     *
     * <p>Failure shape: min {@code expected 5.0} but {@code actual NaN}. The companion max assertion
     * passes today and documents the intended (guarded) behaviour.
     */
    @Test
    public void doubleMinMustIgnoreNaNInputLikeDoubleMax() {
        DoubleMaxFlowFunction max = new DoubleMaxFlowFunction();
        max.aggregateDouble(5.0);
        max.aggregateDouble(Double.NaN);
        assertEquals("Max already guards NaN", 5.0, max.getAsDouble(), 0.0);

        DoubleMinFlowFunction min = new DoubleMinFlowFunction();
        min.aggregateDouble(5.0);
        min.aggregateDouble(Double.NaN);
        assertEquals("Min must ignore NaN symmetrically with Max", 5.0, min.getAsDouble(), 0.0);
    }
}
