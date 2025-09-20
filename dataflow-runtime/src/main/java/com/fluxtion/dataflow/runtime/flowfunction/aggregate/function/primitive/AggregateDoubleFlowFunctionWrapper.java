/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.primitive;

import com.fluxtion.dataflow.runtime.flowfunction.DoubleFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.AggregateDoubleFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.MapFlowFunction;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableSupplier;

public class AggregateDoubleFlowFunctionWrapper<F extends AggregateDoubleFlowFunction<F>>
        extends MapFlowFunction<Double, Double, DoubleFlowFunction> implements DoubleFlowFunction {
    private final SerializableSupplier<F> windowFunctionSupplier;
    private transient final F mapFunction;

    private double result;

    public AggregateDoubleFlowFunctionWrapper(DoubleFlowFunction inputEventStream, SerializableSupplier<F> windowFunctionSupplier) {
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
        result = mapFunction.resetDouble();
    }

    @Override
    protected void mapOperation() {
        result = mapFunction.aggregateDouble(getInputEventStream().getAsDouble());
    }

    @Override
    public double getAsDouble() {
        return result;
    }

    @Override
    public Double get() {
        return getAsDouble();
    }

}
