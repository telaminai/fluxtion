/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.node;

import com.telamin.fluxtion.runtime.callback.*;
import com.telamin.fluxtion.runtime.context.DataFlowContext;
import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import com.telamin.fluxtion.runtime.input.SubscriptionManager;
import com.telamin.fluxtion.runtime.time.Clock;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class MutableDataFlowContext implements DataFlowContext, NamedNode {

    private final transient Map<Object, Object> map = new HashMap<>();
    @Inject
    private final NodeNameLookup nodeNameLookup;
    @Inject
    private final EventProcessorCallbackInternal eventDispatcher;
    @Inject
    private final SubscriptionManager subscriptionManager;
    @Inject
    private final DirtyStateMonitor dirtyStateMonitor;
    @Getter
    @Setter
    private Clock clock = Clock.DEFAULT_CLOCK;
    private InternalEventProcessor eventProcessorCallback;

    public MutableDataFlowContext(
            @AssignToField("nodeNameLookup") NodeNameLookup nodeNameLookup,
            @AssignToField("eventDispatcher") EventProcessorCallbackInternal eventDispatcher,
            @AssignToField("subscriptionManager") SubscriptionManager subscriptionManager,
            @AssignToField("dirtyStateMonitor") DirtyStateMonitor dirtyStateMonitor
    ) {
        this.nodeNameLookup = nodeNameLookup;
        this.eventDispatcher = eventDispatcher;
        this.subscriptionManager = subscriptionManager;
        this.dirtyStateMonitor = dirtyStateMonitor;
    }

    public MutableDataFlowContext() {
        this(null, null, null, null);
    }

    public void replaceMappings(Map<Object, Object> newMap) {
        if (newMap != null) {
            map.clear();
            map.putAll(newMap);
        }
    }

    public <K, V> void addMapping(K key, V value) {
        map.put(key, value);
    }

    public void setEventProcessorCallback(InternalEventProcessor eventProcessorCallback) {
        this.eventProcessorCallback = eventProcessorCallback;
        eventDispatcher.setEventProcessor(eventProcessorCallback);
    }

    @Override
    public NodeNameLookup getNodeNameLookup() {
        return nodeNameLookup;
    }

    @Override
    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    public CallbackDispatcher getCallBackDispatcher() {
        return eventDispatcher;
    }

    @Override
    public DirtyStateMonitor getDirtyStateMonitor() {
        return dirtyStateMonitor;
    }

    @Override
    public SubscriptionManager getSubscriptionManager() {
        return subscriptionManager;
    }

    public Map<Object, Object> getMap() {
        return map;
    }

    public <K, V> V put(K key, V value) {
        return (V) map.put(key, value);
    }

    @Override
    public <T> T getExportedService(Class<T> exportedServiceClass) {
        return eventProcessorCallback.exportedService(exportedServiceClass);
    }

    @Override
    public <T> T getExportedService() {
        return eventProcessorCallback.exportedService();
    }

    @Override
    public <T> T getInjectedInstance(Class<T> instanceClass) {
        return Objects.requireNonNull(
                getContextProperty(instanceClass.getCanonicalName()),
                "no instance injected into context of type:" + instanceClass);
    }

    @Override
    public <T> T getInjectedInstance(Class<T> instanceClass, String name) {
        return Objects.requireNonNull(
                getContextProperty(instanceClass.getCanonicalName() + "_" + name),
                "no instance injected into context of type:" + instanceClass + " named:" + name);
    }

    @Override
    public <T> T getInjectedInstanceAllowNull(Class<T> instanceClass) {
        return getContextProperty(instanceClass.getCanonicalName());
    }

    @Override
    public <T> T getInjectedInstanceAllowNull(Class<T> instanceClass, String name) {
        return getContextProperty(instanceClass.getCanonicalName() + "_" + name);
    }

    @Override
    public <K, V> V getContextProperty(K key) {
        return (V) map.get(key);
    }

    @Override
    public String toString() {
        return "MutableDataFlowContext{" +
                "map=" + map +
                '}';
    }

    @Override
    public String getName() {
        return DataFlowContext.DEFAULT_NODE_NAME;
    }
}
