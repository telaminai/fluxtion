/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.primitive;

public class IntSumFlowFunction extends AbstractIntFlowFunction<IntSumFlowFunction> {

    @Override
    public int aggregateInt(int input) {
        value += input;
        reset = false;
        return getAsInt();
    }

    @Override
    public void combine(IntSumFlowFunction combine) {
        value += combine.value;
    }

    @Override
    public void deduct(IntSumFlowFunction deduct) {
        value -= deduct.value;
    }

    @Override
    public String toString() {
        return "AggregateIntSum{" +
                "value=" + value +
                '}';
    }
}
