/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.primitive;

import com.fluxtion.dataflow.runtime.annotations.Initialise;
import com.fluxtion.dataflow.runtime.annotations.builder.Inject;
import com.fluxtion.dataflow.runtime.callback.DirtyStateMonitor;
import com.fluxtion.dataflow.runtime.flowfunction.DoubleFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.AggregateDoubleFlowFunction;

import java.util.function.BooleanSupplier;

public abstract class AbstractDoubleFlowFunction<T extends AbstractDoubleFlowFunction<T>>
        implements DoubleFlowFunction, AggregateDoubleFlowFunction<T> {

    protected double value;
    @Inject
    public DirtyStateMonitor dirtyStateMonitor;
    private BooleanSupplier dirtySupplier;
    private transient boolean parallelCandidate = false;

    @Initialise
    public void init() {
        dirtySupplier = dirtyStateMonitor.dirtySupplier(this);
    }

    @Override
    public double resetDouble() {
        value = Double.NaN;
        return getAsDouble();
    }

    @Override
    public void parallel() {
        parallelCandidate = true;
    }

    @Override
    public boolean parallelCandidate() {
        return parallelCandidate;
    }

    @Override
    public boolean hasChanged() {
        return dirtySupplier.getAsBoolean();
    }

    @Override
    public Double reset() {
        return resetDouble();
    }

    @Override
    public Double aggregate(Double input) {
        return aggregateDouble(input);
    }

    public Double get() {
        return getAsDouble();
    }

    @Override
    public double getAsDouble() {
        return value;
    }

}
