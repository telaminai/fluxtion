/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

import com.telamin.fluxtion.runtime.annotations.Initialise;
import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import com.telamin.fluxtion.runtime.callback.DirtyStateMonitor;
import com.telamin.fluxtion.runtime.flowfunction.IntFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateIntFlowFunction;

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
        // Clear `reset` AFTER delegating: seeded aggregates (min/max) read `reset` in
        // aggregateInt to take the first value verbatim. Clearing it first defeated that
        // seed and leaked the 0 initial value through groupBy (min returned 0). Sum/count
        // ignore `reset` (0 is their identity), so post-clear preserves their behaviour.
        int result = aggregateInt(input);
        reset = false;
        return result;
    }

    public Integer get() {
        return getAsInt();
    }

    @Override
    public int getAsInt() {
        return value;
    }

}
