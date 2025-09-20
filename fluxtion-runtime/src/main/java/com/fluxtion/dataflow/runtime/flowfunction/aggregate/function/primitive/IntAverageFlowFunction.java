/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.primitive;

public class IntAverageFlowFunction extends AbstractIntFlowFunction<IntAverageFlowFunction> {

    private final DoubleAverageFlowFunction avg = new DoubleAverageFlowFunction();

    @Override
    public void combine(IntAverageFlowFunction add) {
        avg.combine(add.avg);
    }

    @Override
    public void deduct(IntAverageFlowFunction add) {
        avg.deduct(add.avg);
    }

    @Override
    public int aggregateInt(int input) {
        value = (int) avg.aggregateDouble(input);
        return getAsInt();
    }
}
