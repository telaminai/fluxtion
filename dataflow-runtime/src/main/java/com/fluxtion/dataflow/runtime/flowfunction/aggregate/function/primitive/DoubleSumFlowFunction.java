/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.primitive;

public class DoubleSumFlowFunction extends AbstractDoubleFlowFunction<DoubleSumFlowFunction> {

    @Override
    public double aggregateDouble(double input) {
        value += input;
        return getAsDouble();
    }

    @Override
    public double resetDouble() {
        value = 0;
        return getAsDouble();
    }

    @Override
    public void combine(DoubleSumFlowFunction combine) {
        value += combine.value;
    }

    @Override
    public void deduct(DoubleSumFlowFunction deduct) {
        value -= deduct.value;
    }

}
