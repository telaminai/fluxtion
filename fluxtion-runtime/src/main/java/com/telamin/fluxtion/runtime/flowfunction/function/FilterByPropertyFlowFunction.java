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
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;

public class FilterByPropertyFlowFunction<T, P, S extends FlowFunction<T>> extends AbstractFlowFunction<T, T, S> {

    final SerializableFunction<P, Boolean> filterFunction;

    final SerializableFunction<T, P> propertyAccessor;

    transient final String auditInfo;

    public FilterByPropertyFlowFunction(
            S inputEventStream,
            @AssignToField("propertyAccessor") SerializableFunction<T, P> propertyAccessor,
            @AssignToField("filterFunction") SerializableFunction<P, Boolean> filterFunction) {
        super(inputEventStream, filterFunction);
        this.propertyAccessor = propertyAccessor;
        this.filterFunction = filterFunction;
        auditInfo = filterFunction.method().getDeclaringClass().getSimpleName() + "->" + filterFunction.method().getName();
    }

    @OnTrigger
    public boolean filter() {
        boolean filter = isPublishTriggered() || filterFunction.apply(propertyAccessor.apply(getInputEventStream().get()));
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
}