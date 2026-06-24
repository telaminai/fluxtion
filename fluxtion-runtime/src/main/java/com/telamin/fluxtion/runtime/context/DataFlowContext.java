/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
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
     * The {@link com.telamin.fluxtion.runtime.service.ServiceRegistryQuery}
     * for the SEP this context belongs to — the read-only view of
     * {@code @ServiceRegistered} dependencies declared inside the processor.
     *
     * <p>Lets a node introspect its own SEP without going through service
     * re-broadcast machinery. Pair with {@link DataFlowContextListener} and
     * an {@code @Initialise} or {@code @Start} method to run boot-time
     * consistency checks before the first event flows:
     *
     * <pre>{@code
     * public class HealthCheck implements DataFlowContextListener {
     *     private DataFlowContext ctx;
     *     public void currentContext(DataFlowContext c) { this.ctx = c; }
     *
     *     @Start
     *     public void verify() {
     *         ServiceRegistryQuery q = ctx.serviceRegistryQuery().orElseThrow();
     *         // q.serviceDependencies() now reflects the full graph
     *     }
     * }
     * }</pre>
     *
     * <p>Empty when the SEP has no
     * {@link com.telamin.fluxtion.runtime.service.ServiceRegistryNode}
     * (older generated SEPs or hand-built processors that opted out of the
     * standard auditor set).
     *
     * @return query interface, or empty if the registry node is absent
     */
    default java.util.Optional<com.telamin.fluxtion.runtime.service.ServiceRegistryQuery> serviceRegistryQuery() {
        DataFlow parent = getParentDataFlow();
        return parent == null ? java.util.Optional.empty() : parent.serviceRegistryQuery();
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
