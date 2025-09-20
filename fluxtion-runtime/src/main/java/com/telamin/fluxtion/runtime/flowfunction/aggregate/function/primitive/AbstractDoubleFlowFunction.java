/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

import com.telamin.fluxtion.runtime.annotations.Initialise;
import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import com.telamin.fluxtion.runtime.callback.DirtyStateMonitor;
import com.telamin.fluxtion.runtime.flowfunction.DoubleFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateDoubleFlowFunction;

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
