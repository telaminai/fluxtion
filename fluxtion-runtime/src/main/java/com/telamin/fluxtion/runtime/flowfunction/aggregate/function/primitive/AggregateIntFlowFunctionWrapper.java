/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

import com.telamin.fluxtion.runtime.flowfunction.IntFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateIntFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.MapFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;

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
