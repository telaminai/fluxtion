/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.primitive;

public class DoubleMinFlowFunction extends AbstractDoubleFlowFunction<DoubleMinFlowFunction> {

    @Override
    public double aggregateDouble(double input) {
        value = Double.isNaN(value) ? input : Math.min(value, input);
        return getAsDouble();
    }

    @Override
    public void combine(DoubleMinFlowFunction add) {
        aggregateDouble(add.getAsDouble());
    }

    @Override
    public boolean deductSupported() {
        return false;
    }
}
