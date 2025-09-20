/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.node;

import com.telamin.fluxtion.runtime.context.DataFlowContext;
import com.telamin.fluxtion.runtime.context.DataFlowContextListener;
import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.annotations.builder.FluxtionIgnore;
import com.telamin.fluxtion.runtime.audit.EventLogNode;
import com.telamin.fluxtion.runtime.flowfunction.TriggeredFlowFunction;
import com.telamin.fluxtion.runtime.event.NamedFeedEvent;
import lombok.Getter;

import java.util.Objects;
import java.util.function.BooleanSupplier;

public class NamedFeedEventHandlerNode<T>
        extends EventLogNode
        implements
        LifecycleNode,
        NamedNode,
        DataFlowContextListener,
        EventHandlerNode<NamedFeedEvent<?>>,
        TriggeredFlowFunction<NamedFeedEvent<T>> {


    protected final String feedName;
    @FluxtionIgnore
    protected final String name;
    @FluxtionIgnore
    private final EventSubscription<?> subscription;
    @Getter
    protected NamedFeedEvent<T> feedEvent;
    private BooleanSupplier dirtySupplier;
    private DataFlowContext currentContext;


    public NamedFeedEventHandlerNode(
            @AssignToField("feedName") String feedName
    ) {
        this(feedName, "eventFeedHandler_" + feedName);
    }

    public NamedFeedEventHandlerNode(
            @AssignToField("feedName") String feedName,
            @AssignToField("name") String name) {
        Objects.requireNonNull(feedName, "feedName cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        this.feedName = feedName;
        this.name = name;
        subscription = new EventSubscription<>(feedName, Integer.MAX_VALUE, feedName, NamedFeedEvent.class);
    }

    @Override
    public void currentContext(DataFlowContext currentContext) {
        this.currentContext = currentContext;
    }

    @Override
    public void init() {
        dirtySupplier = currentContext.getDirtyStateMonitor().dirtySupplier(this);
        currentContext.getSubscriptionManager().subscribeToNamedFeed(subscription);
    }

    @Override
    public void tearDown() {
        currentContext.getSubscriptionManager().unSubscribeToNamedFeed(subscription);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String filterString() {
        return feedName;
    }

    @Override
    public Class<? extends NamedFeedEvent<?>> eventClass() {
        return (Class<? extends NamedFeedEvent<?>>) (Object) NamedFeedEvent.class;
    }

    @Override
    public <E extends NamedFeedEvent<?>> boolean onEvent(E e) {
        feedEvent = (NamedFeedEvent<T>) e;
        return true;
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

    @Override
    public boolean hasChanged() {
        return dirtySupplier.getAsBoolean();
    }

    @Override
    public void parallel() {

    }

    @Override
    public boolean parallelCandidate() {
        return false;
    }

    @Override
    public NamedFeedEvent<T> get() {
        return feedEvent;
    }
}
