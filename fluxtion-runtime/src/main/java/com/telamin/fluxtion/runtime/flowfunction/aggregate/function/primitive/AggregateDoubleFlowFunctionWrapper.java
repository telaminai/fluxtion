/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

import com.telamin.fluxtion.runtime.flowfunction.DoubleFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateDoubleFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.MapFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;

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
