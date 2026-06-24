/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

public class LongMinFlowFunction extends AbstractLongFlowFunction<LongMinFlowFunction> {

    @Override
    public long aggregateLong(long input) {
        value = reset ? input : Math.min(value, input);
        reset = false;
        return getAsLong();
    }

    @Override
    public void combine(LongMinFlowFunction add) {
        aggregateLong(add.getAsLong());
    }

    @Override
    public boolean deductSupported() {
        return false;
    }
}
