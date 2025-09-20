/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.primitive;

import com.fluxtion.dataflow.runtime.annotations.Initialise;
import com.fluxtion.dataflow.runtime.annotations.builder.Inject;
import com.fluxtion.dataflow.runtime.callback.DirtyStateMonitor;
import com.fluxtion.dataflow.runtime.flowfunction.IntFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.AggregateIntFlowFunction;

import java.util.function.BooleanSupplier;

public abstract class AbstractIntFlowFunction<T extends AbstractIntFlowFunction<T>>
        implements IntFlowFunction, AggregateIntFlowFunction<T> {

    protected int value;
    protected boolean reset = true;
    @Inject
    public DirtyStateMonitor dirtyStateMonitor;
    private BooleanSupplier dirtySupplier;
    private transient boolean parallelCandidate = false;

    @Initialise
    public void init() {
        dirtySupplier = dirtyStateMonitor.dirtySupplier(this);
    }

    @Override
    public Integer reset() {
        return resetInt();
    }

    @Override
    public int resetInt() {
        value = 0;
        reset = true;
        return getAsInt();
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
    public Integer aggregate(Integer input) {
        reset = false;
        return aggregateInt(input);
    }

    public Integer get() {
        return getAsInt();
    }

    @Override
    public int getAsInt() {
        return value;
    }

}
