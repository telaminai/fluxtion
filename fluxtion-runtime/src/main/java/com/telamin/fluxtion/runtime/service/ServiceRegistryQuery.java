/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.service;

import com.telamin.fluxtion.runtime.annotations.feature.Preview;

import java.util.List;
import java.util.Optional;

/**
 * Public, thread-safe, read-only query API over a SEP's service-dependency
 * graph — the {@code @ServiceRegistered} bindings declared by nodes in this
 * processor.
 *
 * <p>Implemented by {@link ServiceRegistryNode}; obtained by callers via the
 * SEP's node lookup (Fluxtion's {@code @ExportService} mechanism is reserved
 * for void-returning command surfaces, so this query interface is exposed
 * through {@code getNodeById} instead):
 *
 * <pre>{@code
 * ServiceRegistryQuery q = (ServiceRegistryQuery)
 *     dataFlow.getNodeById(ServiceRegistryNode.NODE_NAME);
 * }</pre>
 *
 * <p>Callers should depend on this interface, not on
 * {@link ServiceRegistryNode}, so future changes to the node implementation
 * don't break consumers.
 *
 * <p>Use cases: external tooling (mongoose admin UI, IDE plugins, health
 * checks) discovering "which sinks / feeds / services does this processor
 * bind to?" without reflecting on the generated SEP or knowing the internal
 * {@code ServiceRegistryNode.NODE_NAME} lookup string.
 *
 * <p><b>Thread safety.</b> Every method returns an immutable snapshot built
 * at call time. Snapshots are self-consistent and safe to publish across
 * threads. Concurrent {@code registerService} / {@code deRegisterService}
 * calls on the implementing node will not corrupt the returned view, though
 * a snapshot taken mid-registration may include or exclude an in-flight
 * mutation — callers wanting transactional visibility across multiple
 * queries must arrange that externally.
 *
 * <p><b>Mutation safety.</b> Returned lists are unmodifiable, including the
 * {@link ServiceDependency#consumers()} node list — callers cannot reach
 * back into the registry's internal allocation.
 */
@Preview
public interface ServiceRegistryQuery {

    /**
     * Snapshot of every {@code (serviceClass, serviceName)} binding declared
     * by any node in this SEP, with the node instances that declared it.
     *
     * @return immutable list — one entry per declared dependency
     */
    List<ServiceDependency> serviceDependencies();

    /**
     * Snapshot of dependencies whose {@link ServiceDependency#serviceClass()}
     * matches the given class. Equivalent to filtering
     * {@link #serviceDependencies()} but cheaper when callers only care
     * about one service type.
     *
     * @param serviceClass the service type to filter on; must not be {@code null}
     * @return immutable list — possibly empty
     */
    List<ServiceDependency> serviceDependencies(Class<?> serviceClass);

    /**
     * Statically-named binding matching {@code (serviceClass, serviceName)}.
     * The name in the method name is load-bearing — this method only
     * matches single-arg {@code @ServiceRegistered("name")} or
     * {@code @ServiceRegistered} listeners, NOT two-arg listeners (where
     * the bound name is only resolved at runtime).
     *
     * <p>For "any-name" listeners (those returning {@code null} from
     * {@link ServiceDependency#serviceName()}), filter
     * {@link #serviceDependencies(Class)} directly.
     *
     * @param serviceClass the service type; must not be {@code null}
     * @param serviceName  the bound name as it appears in the annotation, or
     *                     the canonical class name when bound via the
     *                     no-arg {@code @ServiceRegistered}; must not be
     *                     {@code null}
     * @return the matching dependency, or empty
     */
    Optional<ServiceDependency> findNamedDependency(Class<?> serviceClass, String serviceName);
}
