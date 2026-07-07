/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

public class IntAverageFlowFunction extends AbstractIntFlowFunction<IntAverageFlowFunction> {

    private int count;
    private int sum;

    @Override
    public int aggregateInt(int input) {
        sum += input;
        count++;
        value = sum / count;
        return getAsInt();
    }

    @Override
    public void combine(IntAverageFlowFunction combine) {
        sum += combine.sum;
        count += combine.count;
        updateAverage();
    }

    @Override
    public void deduct(IntAverageFlowFunction deduct) {
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
    public int resetInt() {
        super.resetInt();
        sum = 0;
        count = 0;
        return getAsInt();
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
        return "IntAverageFlowFunction{" +
                "avg=" + value +
                " count=" + count +
                ", sum=" + sum +
                ", value=" + value +
                ", reset=" + reset +
                '}';
    }
}
