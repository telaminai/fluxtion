/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
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
