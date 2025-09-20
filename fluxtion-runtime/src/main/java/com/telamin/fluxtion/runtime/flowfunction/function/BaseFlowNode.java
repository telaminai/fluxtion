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
import com.telamin.fluxtion.runtime.annotations.OnParentUpdate;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.flowfunction.FlowSupplier;
import com.telamin.fluxtion.runtime.flowfunction.TriggeredFlowFunction;
import com.telamin.fluxtion.runtime.node.BaseNode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

@Getter
@Setter
public abstract class BaseFlowNode<T> extends BaseNode implements TriggeredFlowFunction<T> {

    protected transient boolean overrideUpdateTrigger;
    protected transient boolean overridePublishTrigger;
    protected transient boolean inputStreamTriggered;
    protected transient boolean overrideTriggered;
    protected transient boolean publishTriggered;
    protected transient boolean publishOverrideTriggered;
    protected transient boolean resetTriggered;
    private Object updateTriggerNode;
    private Object publishTriggerNode;
    private Object publishTriggerOverrideNode;
    private Object resetTriggerNode;
    private BooleanSupplier dirtySupplier;
    protected List<FlowSupplier<?>> inputs = new ArrayList<>();
    private final transient Set<FlowSupplier<?>> requiredSet = new HashSet<>();
    private transient boolean allTriggersUpdated = false;

    @Initialise
    public final void initialiseEventStream() {
        overrideUpdateTrigger = updateTriggerNode != null;
        overridePublishTrigger = publishTriggerOverrideNode != null;
        dirtySupplier = getContext().getDirtyStateMonitor().dirtySupplier(this);
        requiredSet.addAll(inputs);
        initialise();
    }

    protected void initialise() {
        //NO-OP
    }

    protected abstract boolean isStatefulFunction();

    @Override
    public boolean parallelCandidate() {
        return false;
    }

    @OnParentUpdate("inputs")
    public void inputUpdated(FlowSupplier<?> inputEventStream) {
        if (!allTriggersUpdated) {
            requiredSet.remove(inputEventStream);
            allTriggersUpdated = requiredSet.isEmpty();
        }
        inputStreamTriggered = !resetTriggered & allTriggersUpdated;
    }

    @OnParentUpdate("updateTriggerNode")
    public void updateTriggerNodeUpdated(Object triggerNode) {
        overrideTriggered = true;
    }

    @OnParentUpdate("publishTriggerNode")
    public final void publishTriggerNodeUpdated(Object triggerNode) {
        publishTriggered = true;
    }

    @OnParentUpdate("publishTriggerOverrideNode")
    public final void publishTriggerOverrideNodeUpdated(Object triggerNode) {
        publishOverrideTriggered = true;
    }

    @OnParentUpdate("resetTriggerNode")
    public final void resetTriggerNodeUpdated(Object triggerNode) {
        resetTriggered = true;
        inputStreamTriggered = false;
        if (isStatefulFunction()) resetOperation();
        requiredSet.addAll(inputs);
    }

    protected abstract void resetOperation();

    @OnTrigger
    public final boolean trigger() {
        if (executeUpdate()) {
            auditLog.info("invokeTriggerOperation", true);
            triggerOperation();
        } else {
            auditLog.info("invokeTriggerOperation", false);
        }
        return fireEventUpdateNotification();
    }

    protected abstract void triggerOperation();

    /**
     * Checks whether an event notification should be fired to downstream nodes. This can be due to any upstream trigger
     * firing including:
     * <ul>
     *     <li>upstream nodes firing a notification</li>
     *     <li>publish trigger firing</li>
     *     <li>update override firing</li>
     * </ul>
     *
     * @return flag indicating fire a notification to child nodes for any upstream change
     */
    protected boolean fireEventUpdateNotification() {
        boolean fireNotification = (!overridePublishTrigger && !overrideUpdateTrigger && inputStreamTriggered)
                | (!overridePublishTrigger && overrideTriggered)
                | publishOverrideTriggered
                | publishTriggered
                | resetTriggered;
        overrideTriggered = false;
        publishTriggered = false;
        publishOverrideTriggered = false;
        resetTriggered = false;
        inputStreamTriggered = false;
        auditLog.info("fireNotification", fireNotification);
        return fireNotification && get() != null;
    }

    /**
     * Checks whether an event notification should be fired to downstream nodes due to:
     * <ul>
     *     <li>upstream nodes firing a notification</li>
     *     <li>update override firing</li>
     * </ul>
     * Requests from publish trigger are not included in this flag
     *
     * @return flag indicating fire a notification to child nodes for an upstream update change, not publish trigger
     */
    protected boolean executeUpdate() {
        return (!overrideUpdateTrigger && inputStreamTriggered) | overrideTriggered;
    }

    @Override
    public void parallel() {

    }

    @Override
    public boolean hasChanged() {
        return dirtySupplier.getAsBoolean();
    }
}
