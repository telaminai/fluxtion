/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

public class DoubleMinFlowFunction extends AbstractDoubleFlowFunction<DoubleMinFlowFunction> {

    // Seed via the NaN sentinel: a fresh instance must start NaN so the first input is
    // taken verbatim. The inherited `value` field defaults to 0.0, which through groupBy
    // (the wrapper supplies a fresh, un-reset function per key) would make min() seed from
    // 0.0 and leak it for all-positive groups. resetDouble() already restores NaN.
    public DoubleMinFlowFunction() {
        value = Double.NaN;
    }

    @Override
    public double aggregateDouble(double input) {
        // Guard NaN symmetrically with DoubleMax: Math.min(x, NaN) == NaN would otherwise poison the
        // running min permanently until reset.
        if (!Double.isNaN(input)) {
            value = Double.isNaN(value) ? input : Math.min(value, input);
        }
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
