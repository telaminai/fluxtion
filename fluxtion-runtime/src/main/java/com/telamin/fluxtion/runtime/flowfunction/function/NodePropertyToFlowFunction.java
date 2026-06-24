/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.function;

import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import com.telamin.fluxtion.runtime.audit.EventLogNode;
import com.telamin.fluxtion.runtime.callback.DirtyStateMonitor;
import com.telamin.fluxtion.runtime.flowfunction.TriggeredFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection;

public class NodePropertyToFlowFunction<T> extends EventLogNode implements TriggeredFlowFunction<T> {

    private transient final Object source;
    private final LambdaReflection.SerializableSupplier<T> methodSupplier;
    private transient T value;
    @Inject
    public DirtyStateMonitor dirtyStateMonitor;

    public NodePropertyToFlowFunction(LambdaReflection.SerializableSupplier<T> methodSupplier) {
        this.methodSupplier = methodSupplier;
        this.source = methodSupplier.captured()[0];
    }

    @OnTrigger
    public boolean triggered() {
        value = methodSupplier.get();
        return true;
    }

    @Override
    public boolean hasChanged() {
        return dirtyStateMonitor.isDirty(this);
    }

    @Override
    public void parallel() {

    }

    @Override
    public boolean parallelCandidate() {
        return false;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public void setUpdateTriggerNode(Object updateTriggerNode) {
        //do nothing
    }

    @Override
    public void setPublishTriggerNode(Object publishTriggerNode) {
        //do nothing
    }

    @Override
    public void setResetTriggerNode(Object resetTriggerNode) {
        //do nothing
    }

    @Override
    public void setPublishTriggerOverrideNode(Object publishTriggerOverrideNode) {
    }
}
