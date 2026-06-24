/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.function;

import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import com.telamin.fluxtion.runtime.partition.MethodReferenceInfo;

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

    public FilterByPropertyFlowFunction(
            S inputEventStream,
            @AssignToField("propertyAccessor") SerializableFunction<T, P> propertyAccessor,
            @AssignToField("filterFunction") SerializableFunction<P, Boolean> filterFunction,
            MethodReferenceInfo methodReferenceInfo) {
        super(inputEventStream, filterFunction, methodReferenceInfo);
        this.propertyAccessor = propertyAccessor;
        this.filterFunction = filterFunction;
        auditInfo = methodReferenceInfo.getAuditName();
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