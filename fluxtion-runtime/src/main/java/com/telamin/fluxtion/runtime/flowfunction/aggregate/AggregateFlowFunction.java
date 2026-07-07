/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate;

import com.telamin.fluxtion.runtime.flowfunction.Stateful;

import java.util.function.Supplier;

/**
 * {@link java.util.function.Function}
 *
 * @param <I> Input type
 * @param <R> Return type of the wrapped function
 * @param <T> The type of this BaseSlidingWindowFunction
 */
public interface AggregateFlowFunction<I, R, T extends AggregateFlowFunction<I, R, T>> extends Stateful<R>, Supplier<R> {

    default void combine(T add) {
        throw new RuntimeException("Sliding not supported implement combine for " + this.getClass().getName());
    }

    /**
     * Subtract the contribution of an expiring window bucket — the inverse of {@link #combine}. The
     * sliding-window engine calls this ONLY when {@link #deductSupported()} returns {@code true}, so
     * any aggregate that opts in MUST implement it.
     */
    default void deduct(T add) {
        throw new UnsupportedOperationException(
                "deduct() not implemented for " + this.getClass().getName()
                        + " — an aggregate whose deductSupported() returns true must implement deduct()");
    }

    /**
     * Whether this aggregate supports {@link #deduct} as a true inverse of {@link #combine} — i.e.
     * whether it is invertible (forms a group), so a sliding window can subtract an expiring bucket
     * in O(Δ) instead of recomputing from the live buckets.
     *
     * <p><b>No default — every aggregate must declare this explicitly.</b> Invertibility is a
     * correctness boundary, so each implementation is required to consciously state its algebra:
     * only invertible (group) aggregates — sum, count, average — return {@code true}; semilattice
     * aggregates (min/max), set/list, distinct, identity and most custom aggregates return
     * {@code false} and route through full recompute. Returning {@code true} obliges a correct
     * {@link #deduct}. (The {@link com.telamin.fluxtion.runtime.flowfunction.aggregate.function.AbstractAggregateFlowFunction}
     * convenience base answers {@code false} for you; override it if your aggregate is invertible.)
     */
    boolean deductSupported();

    R get();

    R aggregate(I input);

}
