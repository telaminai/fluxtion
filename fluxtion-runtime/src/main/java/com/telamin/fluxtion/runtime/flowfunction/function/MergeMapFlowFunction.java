/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.function;

import com.telamin.fluxtion.runtime.annotations.Initialise;
import com.telamin.fluxtion.runtime.annotations.NoTriggerReference;
import com.telamin.fluxtion.runtime.annotations.OnParentUpdate;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.annotations.builder.FluxtionIgnore;
import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import com.telamin.fluxtion.runtime.callback.DirtyStateMonitor;
import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.TriggeredFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Merges multiple event stream into a single transformed output
 */
@ToString
public class MergeMapFlowFunction<T> implements TriggeredFlowFunction<T> {

    private final Supplier<T> factory;
    private T result;
    private final List<MergeProperty<T, ?>> mergeProperties;
    @SuppressWarnings("rawtypes")
    private List<FlowFunction> triggerList = new ArrayList<>();
    @NoTriggerReference
    @SuppressWarnings("rawtypes")
//    @FluxtionIgnore
    private List<FlowFunction> nonTriggeringSources = new ArrayList<>();
    private Set<FlowFunction<?>> requiredSet = new HashSet<>();

    private transient boolean allTriggersUpdated = false;
    @Inject
    public DirtyStateMonitor dirtyStateMonitor;

    public MergeMapFlowFunction(LambdaReflection.SerializableSupplier<T> factory) {
        this(factory, new ArrayList<>());
    }

    public MergeMapFlowFunction(LambdaReflection.SerializableSupplier<T> factory, List<MergeProperty<T, ?>> mergeProperties) {
        this.factory = factory;
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
            result = result == null ? factory.get() : result;
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
        requiredSet.clear();
        mergeProperties.stream()
                .filter(MergeProperty::isMandatory)
                .map(MergeProperty::getTrigger)
                .forEach(requiredSet::add);
        allTriggersUpdated = requiredSet.isEmpty();
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
