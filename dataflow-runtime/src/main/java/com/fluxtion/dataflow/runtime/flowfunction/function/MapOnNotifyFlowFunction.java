/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.function;

import com.fluxtion.dataflow.runtime.annotations.NoTriggerReference;
import com.fluxtion.dataflow.runtime.annotations.OnTrigger;
import com.fluxtion.dataflow.runtime.annotations.PushReference;
import com.fluxtion.dataflow.runtime.annotations.builder.Inject;
import com.fluxtion.dataflow.runtime.flowfunction.FlowFunction;
import com.fluxtion.dataflow.runtime.node.NodeNameLookup;
import lombok.ToString;

@ToString
public class MapOnNotifyFlowFunction<R, T, S extends FlowFunction<R>> extends AbstractFlowFunction<R, T, S> {

    @PushReference
    private final T target;
    private final transient String auditInfo;
    private String instanceNameToNotify;
    @Inject
    @NoTriggerReference
    public NodeNameLookup nodeNameLookup;

    public MapOnNotifyFlowFunction(S inputEventStream, T target) {
        super(inputEventStream, null);
        this.target = target;
        auditInfo = target.getClass().getSimpleName();
    }

    protected void initialise() {
        instanceNameToNotify = nodeNameLookup.lookupInstanceName(target);
    }

    @OnTrigger
    public boolean notifyChild() {
        auditLog.info("notifyClass", auditInfo);
        auditLog.info("notifyInstance", instanceNameToNotify);
        return fireEventUpdateNotification();
    }

    @Override
    public T get() {
        return target;
    }
}
