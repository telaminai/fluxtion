/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.function;

import com.telamin.fluxtion.runtime.annotations.OnParentUpdate;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import com.telamin.fluxtion.runtime.audit.EventLogNode;
import com.telamin.fluxtion.runtime.callback.DirtyStateMonitor;
import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.TriggeredFlowFunction;

import java.util.ArrayList;
import java.util.List;

public class MergeFlowFunction<T, S extends FlowFunction<T>, R extends FlowFunction<? extends T>> extends EventLogNode
        implements TriggeredFlowFunction<T> {

    private final List<FlowFunction<? extends T>> mergeList;

    private T update;
    @Inject
    public DirtyStateMonitor dirtyStateMonitor;

    public MergeFlowFunction(
            S inputEventStream1,
            R inputEventStream2) {
        mergeList = new ArrayList<>();
        mergeList.add(inputEventStream1);
        mergeList.add(inputEventStream2);
    }

    public MergeFlowFunction(List<FlowFunction<? extends T>> mergeList) {
        this.mergeList = mergeList;
    }

    @OnParentUpdate("mergeList")
    public void inputStreamUpdated(FlowFunction<? extends T> inputEventStream1) {
        update = (T) inputEventStream1.get();
    }

    @Override
    public void parallel() {

    }

    @Override
    public boolean parallelCandidate() {
        return false;
    }

    @OnTrigger
    public boolean publishMerge() {
        return update != null;
    }

    @Override
    public boolean hasChanged() {
        return dirtyStateMonitor.isDirty(this);
    }

    @Override
    public T get() {
        return update;
    }

    @Override
    public void setUpdateTriggerNode(Object updateTriggerNode) {

    }

    @Override
    public void setPublishTriggerNode(Object publishTriggerNode) {

    }

    @Override
    public void setResetTriggerNode(Object resetTriggerNode) {

    }

    @Override
    public void setPublishTriggerOverrideNode(Object publishTriggerOverrideNode) {

    }
}
