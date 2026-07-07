/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

public class DoubleMaxFlowFunction extends AbstractDoubleFlowFunction<DoubleMaxFlowFunction> {

    // Seed via the NaN sentinel: a fresh instance must start NaN so the first input is
    // taken verbatim. The inherited `value` field defaults to 0.0, which through groupBy
    // (the wrapper supplies a fresh, un-reset function per key) would make max() seed from
    // 0.0 and leak it for all-negative groups. resetDouble() already restores NaN.
    public DoubleMaxFlowFunction() {
        value = Double.NaN;
    }

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
