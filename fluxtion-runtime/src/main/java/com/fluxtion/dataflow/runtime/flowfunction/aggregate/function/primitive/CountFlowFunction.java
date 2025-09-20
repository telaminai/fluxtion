/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.primitive;

import com.fluxtion.dataflow.runtime.annotations.Initialise;
import com.fluxtion.dataflow.runtime.annotations.builder.Inject;
import com.fluxtion.dataflow.runtime.callback.DirtyStateMonitor;
import com.fluxtion.dataflow.runtime.flowfunction.IntFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.AggregateFlowFunction;

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
