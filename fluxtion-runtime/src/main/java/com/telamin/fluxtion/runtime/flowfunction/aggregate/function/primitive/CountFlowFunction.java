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
import com.telamin.fluxtion.runtime.flowfunction.IntFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;

import java.util.function.BooleanSupplier;

public class CountFlowFunction<T> implements AggregateFlowFunction<T, Integer, CountFlowFunction<T>>, IntFlowFunction {
    private int count;
    @Inject
    private final DirtyStateMonitor dirtyStateMonitor;
    private BooleanSupplier dirtySupplier;

    public CountFlowFunction(DirtyStateMonitor dirtyStateMonitor) {
        this.dirtyStateMonitor = dirtyStateMonitor;
    }

    public CountFlowFunction() {
        this(null);
    }

    @Initialise
    public void init() {
        dirtySupplier = dirtyStateMonitor.dirtySupplier(this);
    }

    @Override
    public Integer reset() {
        count = 0;
        return get();
    }

    @Override
    public void parallel() {

    }

    @Override
    public boolean parallelCandidate() {
        return false;
    }

    @Override
    public int getAsInt() {
        return count;
    }

    @Override
    public Integer get() {
        return getAsInt();
    }

    @Override
    public Integer aggregate(T input) {
        return ++count;
    }

    public int increment(int input) {
        count++;
        return getAsInt();
    }

    @Override
    public void combine(CountFlowFunction<T> add) {
        count += add.count;
    }

    @Override
    public void deduct(CountFlowFunction<T> add) {
        count -= add.count;
    }

    public int increment(double input) {
        return increment(1);
    }

    public int increment(long input) {
        return increment(1);
    }

    @Override
    public boolean hasChanged() {
        return dirtySupplier.getAsBoolean();
    }
}
