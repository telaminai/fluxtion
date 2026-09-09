/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

import com.telamin.fluxtion.runtime.flowfunction.LongFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateLongFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.MapFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;

public class AggregateLongFlowFunctionWrapper<F extends AggregateLongFlowFunction<F>>
        extends MapFlowFunction<Long, Long, LongFlowFunction> implements LongFlowFunction {
    private final SerializableSupplier<F> windowFunctionSupplier;
    private transient final F mapFunction;

    private long result;

    public AggregateLongFlowFunctionWrapper(LongFlowFunction inputEventStream, SerializableSupplier<F> windowFunctionSupplier) {
        super(inputEventStream, null);
        this.windowFunctionSupplier = windowFunctionSupplier;
        this.mapFunction = windowFunctionSupplier.get();
        // aggregateLong, not aggregateInt. Both wrappers were copied from the int one and kept
        // its audit name, so a double or long aggregate reported a method it never called. Nothing in
        // Java could see it: the name is only ever read out of an audit log. Found by comparing the log
        // against the C++ target, which derived the name from the wrapper it was actually emitting.
        auditInfo = mapFunction.getClass().getSimpleName() + "->aggregateLong";
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
