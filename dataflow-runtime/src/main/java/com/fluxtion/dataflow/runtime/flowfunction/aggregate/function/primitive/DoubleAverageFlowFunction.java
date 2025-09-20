/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.primitive;

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
        value = sum / count;
    }

    @Override
    public void deduct(DoubleAverageFlowFunction deduct) {
        sum -= deduct.sum;
        count -= deduct.count;
        value = sum / count;
    }

    @Override
    public double resetDouble() {
        value = 0;
        sum = 0;
        count = 0;
        return 0;
    }
}
