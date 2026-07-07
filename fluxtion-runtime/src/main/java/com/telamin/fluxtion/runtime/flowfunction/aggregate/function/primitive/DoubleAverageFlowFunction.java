/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

public class DoubleAverageFlowFunction extends AbstractDoubleFlowFunction<DoubleAverageFlowFunction> {

    private int count;
    private double sum;

    @Override
    public double aggregateDouble(double input) {
        sum += input;
        count++;
        value = sum / count;
        return getAsDouble();
    }

    @Override
    public void combine(DoubleAverageFlowFunction combine) {
        sum += combine.sum;
        count += combine.count;
        updateAverage();
    }

    @Override
    public void deduct(DoubleAverageFlowFunction deduct) {
        sum -= deduct.sum;
        count -= deduct.count;
        updateAverage();
    }

    /** Invertible: carries (sum, count) so {@code deduct} is a true inverse of {@code combine}. */
    @Override
    public boolean deductSupported() {
        return true;
    }

    @Override
    public double resetDouble() {
        value = 0;
        sum = 0;
        count = 0;
        return 0;
    }

    private void updateAverage() {
        if (count <= 0) {
            count = 0;
            sum = 0;
            value = 0;
        } else {
            value = sum / count;
        }
    }

    @Override
    public String toString() {
        return "DoubleAverageFlowFunction{" +
                "count=" + count +
                ", sum=" + sum +
                ", value=" + value +
                '}';
    }
}
