/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.function;

import com.telamin.fluxtion.runtime.annotations.NoTriggerReference;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.annotations.PushReference;
import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.node.NodeNameLookup;
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
