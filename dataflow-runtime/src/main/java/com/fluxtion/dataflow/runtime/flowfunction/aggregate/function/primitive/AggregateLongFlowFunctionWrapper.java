/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.primitive;

import com.fluxtion.dataflow.runtime.flowfunction.LongFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.AggregateLongFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.MapFlowFunction;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableSupplier;

public class AggregateLongFlowFunctionWrapper<F extends AggregateLongFlowFunction<F>>
        extends MapFlowFunction<Long, Long, LongFlowFunction> implements LongFlowFunction {
    private final SerializableSupplier<F> windowFunctionSupplier;
    private transient final F mapFunction;

    private long result;

    public AggregateLongFlowFunctionWrapper(LongFlowFunction inputEventStream, SerializableSupplier<F> windowFunctionSupplier) {
        super(inputEventStream, null);
        this.windowFunctionSupplier = windowFunctionSupplier;
        this.mapFunction = windowFunctionSupplier.get();
        auditInfo = mapFunction.getClass().getSimpleName() + "->aggregateInt";
    }

    protected void initialise() {
    }

    @Override
    public boolean isStatefulFunction() {
        return true;
    }

    @Override
    protected void resetOperation() {
        result = mapFunction.resetLong();
    }

    @Override
    protected void mapOperation() {
        result = mapFunction.aggregateLong(getInputEventStream().getAsLong());
    }

    @Override
    public long getAsLong() {
        return result;
    }

    @Override
    public Long get() {
        return getAsLong();
    }

}
