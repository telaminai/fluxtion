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
import com.telamin.fluxtion.runtime.flowfunction.function.AbstractFlowFunction.AbstractBinaryEventStream;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableBiFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;

public class FilterByPropertyDynamicFlowFunction<T, P, A, S extends FlowFunction<T>, B extends FlowFunction<A>>
        extends AbstractBinaryEventStream<T, A, T, S, B> {

    private final SerializableFunction<T, P> propertyAccessor;
    private final SerializableBiFunction<P, A, Boolean> filterFunction;
    private transient final String auditInfo;

    public FilterByPropertyDynamicFlowFunction(@AssignToField("inputEventStream") S inputEventStream,
                                               SerializableFunction<T, P> propertyAccessor,
                                               @AssignToField("inputEventStream_2") B inputEventStream_2,
                                               SerializableBiFunction<P, A, Boolean> filterFunction) {
        super(inputEventStream, inputEventStream_2, filterFunction);
        this.propertyAccessor = propertyAccessor;
        this.filterFunction = filterFunction;
        auditInfo = filterFunction.method().getDeclaringClass().getSimpleName() + "->" + filterFunction.method().getName();
    }

    @OnTrigger
    public boolean filter() {
        boolean filter = inputStreamTriggered_1
                & (inputStreamTriggered_2)
                && (isPublishTriggered() || filterFunction.apply(propertyAccessor.apply(getInputEventStream().get()), secondArgument()));
        boolean fireNotification = filter & fireEventUpdateNotification();
        auditLog.info("filterFunction", auditInfo);
        auditLog.info("filterPass", filter);
        auditLog.info("publishToChild", fireNotification);
        return fireNotification;
    }

    @Override
    public T get() {
        return getInputEventStream().get();
    }

    private A secondArgument() {
        return getInputEventStream_2().get();
    }
}