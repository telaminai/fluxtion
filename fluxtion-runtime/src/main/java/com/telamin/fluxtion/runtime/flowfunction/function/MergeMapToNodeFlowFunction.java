/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.function;

import com.telamin.fluxtion.runtime.annotations.Initialise;
import com.telamin.fluxtion.runtime.annotations.NoTriggerReference;
import com.telamin.fluxtion.runtime.annotations.OnParentUpdate;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import com.telamin.fluxtion.runtime.callback.DirtyStateMonitor;
import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.TriggeredFlowFunction;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Merges multiple event stream into a single transformed output
 */
@ToString
public class MergeMapToNodeFlowFunction<T> implements TriggeredFlowFunction<T> {

    private final T result;
    private final List<MergeProperty<T, ?>> mergeProperties;
    @SuppressWarnings("rawtypes")
    private List<FlowFunction> triggerList = new ArrayList<>();
    @NoTriggerReference
    @SuppressWarnings("rawtypes")
    private final transient List<FlowFunction> nonTriggeringSources = new ArrayList<>();
    private final transient Set<FlowFunction<?>> requiredSet = new HashSet<>();
    private transient boolean allTriggersUpdated = false;
    @Inject
    public DirtyStateMonitor dirtyStateMonitor;

    public MergeMapToNodeFlowFunction(T result) {
        this(result, new ArrayList<>());
    }

    public MergeMapToNodeFlowFunction(
            @AssignToField("result") T result,
            @AssignToField("mergeProperties") List<MergeProperty<T, ?>> mergeProperties) {
        this.result = result;
        this.mergeProperties = mergeProperties;
    }

    @OnParentUpdate("triggerList")
    public void inputUpdated(FlowFunction<?> trigger) {
        if (!allTriggersUpdated) {
            requiredSet.remove(trigger);
            allTriggersUpdated = requiredSet.isEmpty();
        }
    }

    @OnParentUpdate("nonTriggeringSources")
    public void inputNonTriggeringUpdated(FlowFunction<?> trigger) {
        if (!allTriggersUpdated) {
            requiredSet.remove(trigger);
            allTriggersUpdated = requiredSet.isEmpty();
        }
    }

    @OnTrigger
    public boolean triggered() {
        if (allTriggersUpdated) {
            for (int i = 0; i < mergeProperties.size(); i++) {
                mergeProperties.get(i).push(result);
            }
        }
        return allTriggersUpdated;
    }

    @Override
    public void parallel() {

    }

    @Override
    public boolean parallelCandidate() {
        return false;
    }

    @Override
    public boolean hasChanged() {
        return dirtyStateMonitor.isDirty(this);
    }

    public <R> void registerTrigger(MergeProperty<T, R> mergeProperty) {
        if (mergeProperty.isTriggering()) {
            triggerList.add(mergeProperty.getTrigger());
        } else {
            nonTriggeringSources.add(mergeProperty.getTrigger());
        }
        mergeProperties.add(mergeProperty);
    }

    @Override
    public T get() {
        return result;
    }

    @Initialise
    public void init() {
        allTriggersUpdated = false;
        triggerList.clear();
        mergeProperties.stream()
                .map(MergeProperty::getTrigger)
                .forEach(requiredSet::add);
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
