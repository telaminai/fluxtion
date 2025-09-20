/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.node;

import com.telamin.fluxtion.runtime.annotations.builder.FluxtionIgnore;
import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import com.telamin.fluxtion.runtime.audit.EventLogNode;
import com.telamin.fluxtion.runtime.callback.DirtyStateMonitor;
import com.telamin.fluxtion.runtime.callback.EventDispatcher;
import com.telamin.fluxtion.runtime.context.DataFlowContext;
import com.telamin.fluxtion.runtime.input.SubscriptionManager;
import com.telamin.fluxtion.runtime.time.Clock;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Implements {@link NamedNode} overriding hashcode and equals using the name as the equality test and hash code seed
 */
public abstract class SingleNamedNode extends EventLogNode implements NamedNode {

    @FluxtionIgnore
    private final String name;
    @Getter
    @Setter
    @Inject
    private DataFlowContext dataFlowContext;

    public SingleNamedNode(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    protected void processReentrantEvent(Object event) {
        getDataFlowContext().getEventDispatcher().processReentrantEvent(event);
    }

    protected void processAsNewEventCycle(Object event) {
        getDataFlowContext().getEventDispatcher().processAsNewEventCycle(event);
    }

    protected void processAsNewEventCycle(Iterable<Object> iterable) {
        getDataFlowContext().getEventDispatcher().processAsNewEventCycle(iterable);
    }

    protected void isDirty(Object node) {
        getDataFlowContext().getDirtyStateMonitor().isDirty(node);
    }

    protected void markDirty(Object node) {
        getDataFlowContext().getDirtyStateMonitor().markDirty(node);
    }

    protected <V> V getContextProperty(String key) {
        return getDataFlowContext().getContextProperty(key);
    }

    protected <T> T getInjectedInstance(Class<T> instanceClass) {
        return getDataFlowContext().getInjectedInstance(instanceClass);
    }

    protected <T> T getInjectedInstance(Class<T> instanceClass, String name) {
        return getDataFlowContext().getInjectedInstance(instanceClass, name);
    }

    protected <T> T getInjectedInstanceAllowNull(Class<T> instanceClass) {
        return getDataFlowContext().getInjectedInstanceAllowNull(instanceClass);
    }

    protected <T> T getInjectedInstanceAllowNull(Class<T> instanceClass, String name) {
        return getDataFlowContext().getInjectedInstanceAllowNull(instanceClass, name);
    }

    protected String lookupInstanceName(Object node) {
        return getDataFlowContext().getNodeNameLookup().lookupInstanceName(node);
    }

    protected <V> V getInstanceById(String instanceId) throws NoSuchFieldException {
        return getDataFlowContext().getNodeNameLookup().getInstanceById(instanceId);
    }

    public NodeNameLookup getNodeNameLookup() {
        return getDataFlowContext().getNodeNameLookup();
    }

    public EventDispatcher getEventDispatcher() {
        return getDataFlowContext().getEventDispatcher();
    }

    public DirtyStateMonitor getDirtyStateMonitor() {
        return getDataFlowContext().getDirtyStateMonitor();
    }

    public SubscriptionManager getSubscriptionManager() {
        return getDataFlowContext().getSubscriptionManager();
    }

    public Clock getClock() {
        return getDataFlowContext().getClock();
    }

    public <K, V> V getContextProperty(K key) {
        return getDataFlowContext().getContextProperty(key);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SingleNamedNode that)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}