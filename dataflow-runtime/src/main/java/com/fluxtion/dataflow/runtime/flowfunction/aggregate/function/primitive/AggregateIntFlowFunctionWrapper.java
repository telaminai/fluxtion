/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.primitive;

import com.fluxtion.dataflow.runtime.flowfunction.IntFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.AggregateIntFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.MapFlowFunction;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableSupplier;

public class AggregateIntFlowFunctionWrapper<F extends AggregateIntFlowFunction<F>>
        extends MapFlowFunction<Integer, Integer, IntFlowFunction>
        implements IntFlowFunction {
    private final SerializableSupplier<F> windowFunctionSupplier;
    private transient final F aggregateFunction;

    private int result;

    public AggregateIntFlowFunctionWrapper(IntFlowFunction inputEventStream, SerializableSupplier<F> windowFunctionSupplier) {
        super(inputEventStream, null);
        this.windowFunctionSupplier = windowFunctionSupplier;
        this.aggregateFunction = windowFunctionSupplier.get();
        auditInfo = aggregateFunction.getClass().getSimpleName() + "->aggregateInt";
    }

    protected void initialise() {
    }

    @Override
    public boolean isStatefulFunction() {
        return true;
    }

    @Override
    protected void resetOperation() {
        result = aggregateFunction.resetInt();
    }

    @Override
    protected void mapOperation() {
        result = aggregateFunction.aggregateInt(getInputEventStream().getAsInt());
    }

    @Override
    public int getAsInt() {
        return result;
    }

    @Override
    public Integer get() {
        return getAsInt();
    }

}
