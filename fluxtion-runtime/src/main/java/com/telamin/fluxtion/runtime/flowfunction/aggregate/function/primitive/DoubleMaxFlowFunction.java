/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

public class DoubleMaxFlowFunction extends AbstractDoubleFlowFunction<DoubleMaxFlowFunction> {

    @Override
    public double aggregateDouble(double input) {
        if (!Double.isNaN(input)) {
            value = Double.isNaN(value) ? input : Math.max(value, input);
        }
        return getAsDouble();
    }

    @Override
    public void combine(DoubleMaxFlowFunction add) {
        aggregateDouble(add.getAsDouble());
    }

    @Override
    public boolean deductSupported() {
        return false;
    }
}
