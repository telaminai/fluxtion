/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

public class IntMinFlowFunction extends AbstractIntFlowFunction<IntMinFlowFunction> {

    @Override
    public int aggregateInt(int input) {
        value = reset ? input : Math.min(value, input);
        reset = false;
        return getAsInt();
    }

    @Override
    public void combine(IntMinFlowFunction add) {
        aggregateInt(add.getAsInt());
    }

    @Override
    public boolean deductSupported() {
        return false;
    }
}
