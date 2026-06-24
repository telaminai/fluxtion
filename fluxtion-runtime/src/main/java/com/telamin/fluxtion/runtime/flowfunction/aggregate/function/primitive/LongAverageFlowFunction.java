/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

public class LongAverageFlowFunction extends AbstractLongFlowFunction<LongAverageFlowFunction> {

    private int count;
    private long sum;

    @Override
    public long aggregateLong(long input) {
        sum += input;
        count++;
        value = sum / count;
        return getAsLong();
    }

    @Override
    public void combine(LongAverageFlowFunction combine) {
        sum += combine.sum;
        count += combine.count;
        updateAverage();
    }

    @Override
    public void deduct(LongAverageFlowFunction deduct) {
        sum -= deduct.sum;
        count -= deduct.count;
        updateAverage();
    }

    @Override
    public long resetLong() {
        super.resetLong();
        sum = 0;
        count = 0;
        return getAsLong();
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
        return "LongAverageFlowFunction{" +
                "avg=" + value +
                " count=" + count +
                ", sum=" + sum +
                ", value=" + value +
                ", reset=" + reset +
                '}';
    }
}
