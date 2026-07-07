/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

public class LongSumFlowFunction extends AbstractLongFlowFunction<LongSumFlowFunction> {

    @Override
    public long aggregateLong(long input) {
        value += input;
        reset = false;
        return getAsLong();
    }

    @Override
    public void combine(LongSumFlowFunction combine) {
        value += combine.value;
    }

    @Override
    public void deduct(LongSumFlowFunction deduct) {
        value -= deduct.value;
    }

    /** Invertible (group aggregate): {@code deduct} is a true inverse of {@code combine}. */
    @Override
    public boolean deductSupported() {
        return true;
    }

}
