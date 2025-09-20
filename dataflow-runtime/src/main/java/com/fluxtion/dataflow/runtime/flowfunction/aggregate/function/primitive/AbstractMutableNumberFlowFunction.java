/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.primitive;

import com.fluxtion.dataflow.runtime.flowfunction.MutableNumber;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.AggregateFlowFunction;

public abstract class AbstractMutableNumberFlowFunction
        implements AggregateFlowFunction<Number, Number, AbstractMutableNumberFlowFunction> {
    MutableNumber mutableNumber = new MutableNumber();

    @Override
    public Number reset() {
        return mutableNumber.reset();
    }

    @Override
    public void combine(AbstractMutableNumberFlowFunction add) {

    }

    @Override
    public void deduct(AbstractMutableNumberFlowFunction add) {

    }

    @Override
    public Number get() {
        return mutableNumber;
    }

    @Override
    public Number aggregate(Number input) {
        return null;
    }
}
