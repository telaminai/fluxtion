/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.primitive;

public class IntMaxFlowFunction extends AbstractIntFlowFunction<IntMaxFlowFunction> {

    @Override
    public int aggregateInt(int input) {
        value = reset ? input : Math.max(value, input);
        reset = false;
        return getAsInt();
    }

    @Override
    public void combine(IntMaxFlowFunction add) {
        aggregateInt(add.getAsInt());
    }

    @Override
    public boolean deductSupported() {
        return false;
    }
}
