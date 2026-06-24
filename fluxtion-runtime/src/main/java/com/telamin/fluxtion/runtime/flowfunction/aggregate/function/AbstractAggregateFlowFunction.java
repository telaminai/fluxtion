/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function;

import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;

/**
 * extend this class to make writing an {@link AggregateFlowFunction} a little easier
 *
 * @param <I>
 * @param <R>
 */
public abstract class AbstractAggregateFlowFunction<I, R>
        implements
        AggregateFlowFunction<I, R, AbstractAggregateFlowFunction<I, R>> {

    protected R result;

    @Override
    public R get() {
        return result;
    }

    @Override
    public final R aggregate(I input) {
        result = calculateAggregate(input, get());
        return result;
    }

    @Override
    public final R reset() {
        return result = resetAction(get());
    }

    abstract protected R calculateAggregate(I input, R previous);

    abstract protected R resetAction(R previous);
}