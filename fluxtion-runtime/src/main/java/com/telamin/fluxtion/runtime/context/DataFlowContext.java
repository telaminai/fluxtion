/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.context;

import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.callback.DirtyStateMonitor;
import com.telamin.fluxtion.runtime.callback.EventDispatcher;
import com.telamin.fluxtion.runtime.input.EventFeed;
import com.telamin.fluxtion.runtime.input.SubscriptionManager;
import com.telamin.fluxtion.runtime.node.NodeNameLookup;
import com.telamin.fluxtion.runtime.time.Clock;

/**
 * Runtime access to various services in the running CloneableDataFlow instance.
 */
public interface DataFlowContext {
    String DEFAULT_NODE_NAME = "context";

    NodeNameLookup getNodeNameLookup();

    EventDispatcher getEventDispatcher();

    DirtyStateMonitor getDirtyStateMonitor();

    SubscriptionManager getSubscriptionManager();

    Clock getClock();

    <T> T getExportedService(Class<T> exportedServiceClass);

    <T> T getExportedService();

    /**
     * Retrieves an injected instance at runtime. Fails with {@link RuntimeException} if no instance is found
     * <p>
     * see {@link DataFlow#injectInstance(Object)}
     *
     * @param instanceClass The class of the instance to retrieve
     * @param <T>           The type of the returned class
     * @return The instance injected.
     */
    <T> T getInjectedInstance(Class<T> instanceClass);

    /**
     * Retrieves an injected instance at runtime. Fails with {@link RuntimeException} if no instance is found
     * <p>
     * see {@link DataFlow#injectNamedInstance(Object, String)}
     *
     * @param instanceClass The class of the instance to retrieve
     * @param <T>           The type of the returned class
     * @return The instance injected.
     */
    <T> T getInjectedInstance(Class<T> instanceClass, String name);


    /**
     * Retrieves an injected instance at runtime.
     * <p>
     * see {@link DataFlow#injectInstance(Object)}
     *
     * @param instanceClass The class of the instance to retrieve
     * @param <T>           The type of the returned class
     * @return The instance injected.
     */
    <T> T getInjectedInstanceAllowNull(Class<T> instanceClass);

    /**
     * Retrieves an injected instance at runtime.
     * <p>
     * see {@link DataFlow#injectNamedInstance(Object, String)}
     *
     * @param instanceClass The class of the instance to retrieve
     * @param <T>           The type of the returned class
     * @return The instance injected.
     */
    <T> T getInjectedInstanceAllowNull(Class<T> instanceClass, String name);

    <K, V> V getContextProperty(K key);


    /**
     * The public {@link DataFlow} instance for this context
     *
     * @return Encapsulating DataFlow
     */
    default DataFlow getParentDataFlow() {
        return getExportedService(DataFlow.class);
    }

    /**
     * Helper method for {@link EventDispatcher#processReentrantEvent(Object)}
     *
     * @param event to dispatch to this {@link DataFlow}
     */
    default void processReentrantEvent(Object event) {
        getEventDispatcher().processReentrantEvent(event);
    }

    /**
     * Helper method for {@link EventDispatcher#processAsNewEventCycle(Object)} (Object)}
     *
     * @param event to dispatch to this {@link DataFlow}
     */
    default void processAsNewEventCycle(Object event) {
        getEventDispatcher().processAsNewEventCycle(event);
    }

    /**
     * Helper method for {@link EventDispatcher#processReentrantEvents(Iterable)}
     *
     * @param iterable to dispatch to this {@link DataFlow}
     */
    default void processAsNewEventCycle(Iterable<Object> iterable) {
        getEventDispatcher().processAsNewEventCycle(iterable);
    }

    /**
     * Helper method for {@link DirtyStateMonitor#isDirty(Object)}}
     *
     * @param node to check for dirty state
     */
    default void isDirty(Object node) {
        getDirtyStateMonitor().isDirty(node);
    }

    /**
     * Helper method for {@link DirtyStateMonitor#markDirty(Object)} (Object)}}
     *
     * @param node to mark as dirty during this event cycle
     */
    default void markDirty(Object node) {
        getDirtyStateMonitor().markDirty(node);
    }

    /**
     * Helper method for {@link NodeNameLookup#lookupInstanceName(Object)}
     *
     * @param node the node whose name to lookup
     * @return the name of the node
     */
    default String lookupInstanceName(Object node) {
        return getNodeNameLookup().lookupInstanceName(node);
    }

    /**
     * Helper method for {@link NodeNameLookup#getInstanceById(String)}}
     *
     * @param instanceId used to look up a node instance
     * @return the node whose name matches the supplied predicate
     */
    default <V> V getInstanceById(String instanceId) throws NoSuchFieldException {
        return getNodeNameLookup().getInstanceById(instanceId);
    }

    /**
     * Creates a subscription to {@link EventFeed} registered with a matching feed name
     *
     * @param feedName the name to match on a registered {@link EventFeed}
     */
    default void subscribeToNamedFeed(String feedName) {
        getSubscriptionManager().subscribeToNamedFeed(feedName);
    }

    /**
     * Removes a subscription to {@link EventFeed} registered with a matching feed name
     *
     * @param feedName the name to match on a registered {@link EventFeed}
     */
    default void unSubscribeToNamedFeed(String feedName) {
        getSubscriptionManager().unSubscribeToNamedFeed(feedName);
    }
}
