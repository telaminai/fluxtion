/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function;

import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.MapFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;

import java.util.function.Supplier;

public class AggregateFlowFunctionWrapper<T, R, S extends FlowFunction<T>, F extends AggregateFlowFunction<T, R, F>>
        extends MapFlowFunction<T, R, S> {

    private final Supplier<F> windowFunctionSupplier;
    private transient final F mapFunction;

    public AggregateFlowFunctionWrapper(S inputEventStream, SerializableSupplier<F> windowFunctionSupplier) {
        super(inputEventStream, null);
        this.windowFunctionSupplier = windowFunctionSupplier;
        this.mapFunction = windowFunctionSupplier.get();
//        Anchor.anchorCaptured(this, windowFunctionSupplier);
        auditInfo = mapFunction.getClass().getSimpleName() + "->aggregate";
    }

    protected void initialise() {
    }

    @Override
    public boolean isStatefulFunction() {
        return true;
    }

    @Override
    protected void resetOperation() {
        result = mapFunction.reset();
    }

    @Override
    protected void mapOperation() {
        result = mapFunction.aggregate(getInputEventStream().get());
    }


}
