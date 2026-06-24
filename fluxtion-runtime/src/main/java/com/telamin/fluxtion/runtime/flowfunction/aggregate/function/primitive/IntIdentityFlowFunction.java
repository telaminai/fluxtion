/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

public class IntIdentityFlowFunction extends AbstractIntFlowFunction<IntIdentityFlowFunction> {

    @Override
    public int aggregateInt(int input) {
        value = input;
        return getAsInt();
    }

    @Override
    public void combine(IntIdentityFlowFunction combine) {
        value = combine.value;
    }

    @Override
    public boolean deductSupported() {
        return false;
    }
}