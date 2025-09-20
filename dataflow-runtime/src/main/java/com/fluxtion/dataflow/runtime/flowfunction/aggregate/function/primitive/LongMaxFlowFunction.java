/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.primitive;

public class LongMaxFlowFunction extends AbstractLongFlowFunction<LongMaxFlowFunction> {

    @Override
    public long aggregateLong(long input) {
        value = reset ? input : Math.max(value, input);
        reset = false;
        return getAsLong();
    }

    @Override
    public void combine(LongMaxFlowFunction add) {
        aggregateLong(add.getAsLong());
    }

    @Override
    public boolean deductSupported() {
        return false;
    }
}
