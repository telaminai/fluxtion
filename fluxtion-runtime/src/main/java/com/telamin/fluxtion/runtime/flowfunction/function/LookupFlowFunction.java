/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.function;

import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection;

/**
 * Lookup a value from a memeber variable on the source
 *
 * @param <R> Type of input stream
 * @param <T> Output type of this stream
 * @param <S> The type of {@link FlowFunction} that wraps R
 */
public class LookupFlowFunction<R, T, S extends FlowFunction<R>, I, L> extends AbstractFlowFunction<R, T, S> {

    private final LambdaReflection.SerializableBiFunction<R, L, T> mapFunction;
    private final LambdaReflection.SerializableFunction<I, L> lookupFunction;
    private final LambdaReflection.SerializableFunction<R, I> lookupKeyFunction;
    private T streamOutputValue;

    public LookupFlowFunction(S inputEventStream,
                              @AssignToField("lookupKeyFunction")
                              LambdaReflection.SerializableFunction<R, I> lookupKeyFunction,
                              @AssignToField("lookupFunction")
                              LambdaReflection.SerializableFunction<I, L> lookupFunction,
                              @AssignToField("mapFunction")
                              LambdaReflection.SerializableBiFunction<R, L, T> methodReferenceReflection
    ) {
        super(inputEventStream, methodReferenceReflection);
        this.mapFunction = methodReferenceReflection;
        this.lookupKeyFunction = lookupKeyFunction;
        this.lookupFunction = lookupFunction;
    }

    @OnTrigger
    public boolean applyLookup() {
        R streamValue = getInputEventStream().get();
        I lookupKey = lookupKeyFunction.apply(streamValue);
        if (lookupKey != null) {
            L lookupValue = lookupFunction.apply(lookupKey);
            streamOutputValue = mapFunction.apply(streamValue, lookupValue);
        }
        boolean filter = isPublishTriggered() || lookupKey != null;
        boolean fireNotification = filter & fireEventUpdateNotification();
//        auditLog.info("filterFunction", auditInfo);
        auditLog.info("foundLookupValue", filter);
        auditLog.info("publishToChild", fireNotification);
        return fireNotification;
    }

    @Override
    public T get() {
        return streamOutputValue;
    }
}
