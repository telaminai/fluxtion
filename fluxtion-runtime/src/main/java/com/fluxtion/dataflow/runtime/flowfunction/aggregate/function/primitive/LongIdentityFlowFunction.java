/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.primitive;

public class LongIdentityFlowFunction extends AbstractLongFlowFunction<LongIdentityFlowFunction> {

    @Override
    public long aggregateLong(long input) {
        value = input;
        return getAsLong();
    }

    @Override
    public void combine(LongIdentityFlowFunction combine) {
        value = combine.value;
    }

    @Override
    public boolean deductSupported() {
        return false;
    }

}