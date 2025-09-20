/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.node;

import com.fluxtion.dataflow.runtime.context.DataFlowContext;
import com.fluxtion.dataflow.runtime.context.DataFlowContextListener;
import com.fluxtion.dataflow.runtime.annotations.builder.AssignToField;
import com.fluxtion.dataflow.runtime.annotations.builder.FluxtionIgnore;
import com.fluxtion.dataflow.runtime.audit.EventLogNode;
import com.fluxtion.dataflow.runtime.flowfunction.TriggeredFlowFunction;
import com.fluxtion.dataflow.runtime.event.NamedFeedEvent;
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
