/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.function;

import com.telamin.fluxtion.runtime.annotations.Initialise;
import com.telamin.fluxtion.runtime.annotations.NoTriggerReference;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import com.telamin.fluxtion.runtime.audit.EventLogNode;
import com.telamin.fluxtion.runtime.callback.DirtyStateMonitor;
import com.telamin.fluxtion.runtime.flowfunction.TriggeredFlowFunction;
import com.telamin.fluxtion.runtime.node.NodeNameLookup;

public class NodeToFlowFunction<T> extends EventLogNode implements TriggeredFlowFunction<T> {

    private final T source;
    private String instanceName;
    @Inject
    @NoTriggerReference
    public NodeNameLookup nodeNameLookup;
    @Inject
    public DirtyStateMonitor dirtyStateMonitor;

    public NodeToFlowFunction(T source) {
        this.source = source;
    }

    @OnTrigger
    public void sourceUpdated() {
        auditLog.info("sourceInstance", instanceName);
    }

    @Initialise
    public void init() {
        instanceName = nodeNameLookup.lookupInstanceName(source);
    }

    @Override
    public boolean hasChanged() {
        return dirtyStateMonitor.isDirty(this);
    }

    @Override
    public T get() {
        return source;
    }

    @Override
    public void parallel() {

    }

    @Override
    public boolean parallelCandidate() {
        return false;
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
