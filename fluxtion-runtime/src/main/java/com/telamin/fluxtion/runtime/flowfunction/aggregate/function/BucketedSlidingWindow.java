/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function;

import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateDoubleFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateIntFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateLongFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @param <T> Input type
 * @param <R> return type
 * @param <F> BaseSlidingWindowFunction
 */
public class BucketedSlidingWindow<T, R, F extends AggregateFlowFunction<T, R, F>> {

    private final Supplier<F> windowFunctionSupplier;
    protected final F aggregatedFunction;
    protected final F currentFunction;
    private final List<F> buckets;
    private int writePointer;
    private boolean allBucketsFilled = false;
    private final boolean deductSupported;

    public BucketedSlidingWindow(Supplier<F> windowFunctionSupplier, int numberOfBuckets) {
        this.windowFunctionSupplier = windowFunctionSupplier;
        aggregatedFunction = windowFunctionSupplier.get();
        currentFunction = windowFunctionSupplier.get();
        deductSupported = currentFunction.deductSupported();
        buckets = new ArrayList<>(numberOfBuckets);
        for (int i = 0; i < numberOfBuckets; i++) {
            buckets.add(windowFunctionSupplier.get());
        }
    }

    public void init() {
        aggregatedFunction.reset();
        currentFunction.reset();
        buckets.forEach(AggregateFlowFunction::reset);
    }

    public final void aggregate(T input) {
        currentFunction.aggregate(input);
    }

    public void roll() {
        roll(1);
    }

    public void roll(int windowsToRoll) {
        if (deductSupported) {
            for (int i = 0; i < windowsToRoll; i++) {
                F oldFunction = buckets.get(writePointer);
                aggregatedFunction.combine(currentFunction);
                aggregatedFunction.deduct(oldFunction);
                oldFunction.reset();
                oldFunction.combine(currentFunction);
                currentFunction.reset();
                writePointer++;
                allBucketsFilled = allBucketsFilled | writePointer == buckets.size();
                writePointer = writePointer % buckets.size();
            }
            // A non-finite (NaN/±Infinity) contribution poisons the running sum irrecoverably under
            // deduct (NaN - NaN = NaN, Inf - Inf = NaN), so once its bucket expires the incremental
            // aggregate stays poisoned. Recompute from the live buckets — the same route the
            // non-invertible (min/max) path always takes — so the window recovers. Cheap: the check is
            // O(1) and the recompute only runs while a non-finite value is live or just expired.
            if (isNonFinite(aggregatedFunction.get())) {
                aggregatedFunction.reset();
                for (int i = 0; i < buckets.size(); i++) {
                    aggregatedFunction.combine(buckets.get(i));
                }
            }
        } else {
            aggregatedFunction.reset();
            //clear and then combine
            for (int i = 0; i < windowsToRoll; i++) {
                F oldFunction = buckets.get(writePointer);
                oldFunction.reset();
                oldFunction.combine(currentFunction);
                currentFunction.reset();
                writePointer++;
                allBucketsFilled = allBucketsFilled | writePointer == buckets.size();
                writePointer = writePointer % buckets.size();
            }
            for (int i = 0; i < buckets.size(); i++) {
                aggregatedFunction.combine(buckets.get(i));
            }
        }
    }

    public boolean isAllBucketsFilled() {
        return allBucketsFilled;
    }

    public R get() {
        return aggregatedFunction.get();
    }

    /**
     * Floating-point NaN/infinity are not algebraically invertible under {@code deduct}: once a running
     * sum/average has been poisoned by a non-finite contribution, {@code total -= old} cannot repair it.
     * A non-{@code Double}/{@code Float} aggregate (int/long sums, min/max) is never non-finite here. For a
     * <b>grouped</b> sliding window the aggregate is a {@link GroupBy}, so any per-key value being non-finite
     * poisons that key — scan the values (this runs once per roll, not per event, so it is cheap).
     */
    private static boolean isNonFinite(Object value) {
        if (value instanceof Double) {
            return !Double.isFinite((Double) value);
        }
        if (value instanceof Float) {
            return !Float.isFinite((Float) value);
        }
        if (value instanceof GroupBy) {
            for (Object v : ((GroupBy<?, ?>) value).values()) {
                if (isNonFinite(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static class BucketedSlidingWindowedIntFunction<F extends AggregateIntFlowFunction<F>>
            extends BucketedSlidingWindow<Integer, Integer, F> {

        public BucketedSlidingWindowedIntFunction(SerializableSupplier<F> windowFunctionSupplier, int numberOfBuckets) {
            super(windowFunctionSupplier, numberOfBuckets);
        }

        public void aggregateInt(int input) {
            currentFunction.aggregateInt(input);
        }

        public int getAsInt() {
            return aggregatedFunction.getAsInt();
        }
    }

    public static class BucketedSlidingWindowedDoubleFunction<F extends AggregateDoubleFlowFunction<F>>
            extends BucketedSlidingWindow<Double, Double, F> {

        public BucketedSlidingWindowedDoubleFunction(SerializableSupplier<F> windowFunctionSupplier, int numberOfBuckets) {
            super(windowFunctionSupplier, numberOfBuckets);
        }

        public void aggregateDouble(double input) {
            currentFunction.aggregateDouble(input);
        }

        public double getAsDouble() {
            return aggregatedFunction.getAsDouble();
        }
    }

    public static class BucketedSlidingWindowedLongFunction<F extends AggregateLongFlowFunction<F>>
            extends BucketedSlidingWindow<Long, Long, F> {

        public BucketedSlidingWindowedLongFunction(SerializableSupplier<F> windowFunctionSupplier, int numberOfBuckets) {
            super(windowFunctionSupplier, numberOfBuckets);
        }

        public void aggregateLong(long input) {
            currentFunction.aggregateLong(input);
        }

        public long getAsLong() {
            return aggregatedFunction.getAsLong();
        }
    }

}
