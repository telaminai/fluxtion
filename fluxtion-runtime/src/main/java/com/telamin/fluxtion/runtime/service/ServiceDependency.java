/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.service;

import com.telamin.fluxtion.runtime.annotations.feature.Preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of one {@code @ServiceRegistered} dependency declared
 * inside a SEP. Returned by {@link ServiceRegistryQuery#serviceDependencies()}
 * and friends.
 *
 * <p>Two binding shapes are represented; both map to the same record type
 * distinguished by {@link #serviceName()}:
 *
 * <ul>
 *   <li><b>Statically named</b> — {@code @ServiceRegistered("svc_A")} or
 *       {@code @ServiceRegistered}: {@code serviceName} is the explicit
 *       annotation value or — when unset — the canonical name of the
 *       service class (matching Fluxtion's internal lookup key).</li>
 *   <li><b>Two-arg callback</b> — {@code @ServiceRegistered public void on(MyService s, String name)}:
 *       the bound name resolves at runtime, so {@code serviceName} is
 *       {@code null}.</li>
 * </ul>
 *
 * <p>{@link #consumers()} returns the node(s) that declared the listener,
 * each paired with its registered node id ({@link ConsumerNode#nodeName()}).
 * The list is immutable.
 *
 * <p>Equality is value-based on the (serviceClass, serviceName) pair plus
 * an identity-based comparison of the consumer node references — two
 * structurally-equivalent dependency entries from different SEPs will
 * report unequal because the underlying node instances differ.
 */
@Preview
public final class ServiceDependency {

    private final Class<?> serviceClass;
    private final String serviceName;
    private final List<ConsumerNode> consumers;

    public ServiceDependency(Class<?> serviceClass, String serviceName,
                             List<ConsumerNode> consumers) {
        this.serviceClass = Objects.requireNonNull(serviceClass, "serviceClass");
        this.serviceName = serviceName;
        this.consumers = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(consumers, "consumers")));
    }

    public Class<?> serviceClass()        { return serviceClass; }
    /** {@code null} when this dependency was declared via the two-arg
     *  {@code @ServiceRegistered (service, String name)} listener shape —
     *  the bound name is only known at runtime, after a matching service
     *  is registered. */
    public String serviceName()           { return serviceName; }
    /** Nodes in this SEP that declared {@code @ServiceRegistered} for this
     *  binding, each with its registered node id. Deduplicated by node
     *  instance — a node declaring multiple listener methods for the same
     *  {@code (serviceClass, serviceName)} pair appears once. */
    public List<ConsumerNode> consumers() { return consumers; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceDependency)) return false;
        ServiceDependency that = (ServiceDependency) o;
        return serviceClass.equals(that.serviceClass)
                && Objects.equals(serviceName, that.serviceName)
                && consumers.equals(that.consumers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceClass, serviceName, consumers);
    }

    @Override
    public String toString() {
        StringBuilder names = new StringBuilder("[");
        for (int i = 0; i < consumers.size(); i++) {
            if (i > 0) names.append(", ");
            names.append(consumers.get(i));
        }
        names.append("]");
        return "ServiceDependency{" + serviceClass.getSimpleName()
                + (serviceName == null ? " (anyName)" : ", name=" + serviceName)
                + ", consumers=" + names + "}";
    }

    /**
     * One node binding within a {@link ServiceDependency} — the node
     * instance that declared {@code @ServiceRegistered} plus the id it was
     * registered under. Naming the node enables UI labels, graphml edges
     * and log lines without forcing callers to reach back through
     * {@code NodeNameLookup}.
     *
     * <p>Equality is identity-based on the node reference (two distinct
     * node instances with the same name are not equal).
     */
    @Preview
    public static final class ConsumerNode {
        private final Object node;
        private final String nodeName;

        public ConsumerNode(Object node, String nodeName) {
            this.node = Objects.requireNonNull(node, "node");
            this.nodeName = nodeName;
        }

        /** The node instance — typically a user-added node, occasionally a
         *  built-in auditor. Type erased; callers can {@code instanceof}-cast
         *  if they need the concrete class. */
        public Object node()     { return node; }
        /** Registered id of the node within the SEP, or {@code null} when
         *  the node was added anonymously. */
        public String nodeName() { return nodeName; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ConsumerNode)) return false;
            ConsumerNode that = (ConsumerNode) o;
            return node == that.node;   // identity-based
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(node);
        }

        @Override
        public String toString() {
            return nodeName != null ? nodeName : "<anonymous>";
        }
    }
}
