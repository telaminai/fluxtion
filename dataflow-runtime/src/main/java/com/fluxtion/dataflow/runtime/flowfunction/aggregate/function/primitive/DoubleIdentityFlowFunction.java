/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.primitive;

public class DoubleIdentityFlowFunction extends AbstractDoubleFlowFunction<DoubleIdentityFlowFunction> {

    @Override
    public double aggregateDouble(double input) {
        value = input;
        return getAsDouble();
    }

    @Override
    public void combine(DoubleIdentityFlowFunction combine) {
        value = combine.value;
    }

    @Override
    public boolean deductSupported() {
        return false;
    }

}