/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;

/**
 * Average over any {@link Number} input that returns an exact {@code Double}.
 *
 * <p>Use this for SQL {@code AVG}, which is always double-valued: an {@code int}/{@code long}
 * column averages without the lossy integer division of {@link IntAverageFlowFunction} /
 * {@link LongAverageFlowFunction}, and a {@code double} column averages directly. Because the
 * input is {@code Number}, the natural component accessor (e.g. {@code Trade::qty}) binds
 * without a widening step — through {@code groupBy} the value is boxed anyway, so there is no
 * extra boxing versus {@link DoubleAverageFlowFunction}.
 *
 * <p>Carries {@code (sum, count)} so it {@link #combine}s and {@link #deduct}s for
 * windowed / parallel aggregation.
 */
public class NumberAverageFlowFunction
        implements AggregateFlowFunction<Number, Double, NumberAverageFlowFunction> {

    private double sum;
    private int count;
    private double average;

    @Override
    public Double aggregate(Number input) {
        sum += input.doubleValue();
        count++;
        average = sum / count;
        return average;
    }

    @Override
    public Double get() {
        return average;
    }

    @Override
    public Double reset() {
        sum = 0;
        count = 0;
        average = 0;
        return average;
    }

    @Override
    public void combine(NumberAverageFlowFunction add) {
        sum += add.sum;
        count += add.count;
        updateAverage();
    }

    @Override
    public void deduct(NumberAverageFlowFunction sub) {
        sum -= sub.sum;
        count -= sub.count;
        updateAverage();
    }

    private void updateAverage() {
        if (count <= 0) {
            count = 0;
            sum = 0;
            average = 0;
        } else {
            average = sum / count;
        }
    }

    @Override
    public String toString() {
        return "NumberAverageFlowFunction{sum=" + sum + ", count=" + count + ", average=" + average + '}';
    }
}
