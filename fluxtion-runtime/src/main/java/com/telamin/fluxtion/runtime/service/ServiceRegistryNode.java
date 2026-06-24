/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.service;

import com.telamin.fluxtion.runtime.context.DataFlowContextListener;
import com.telamin.fluxtion.runtime.annotations.ExportService;
import com.telamin.fluxtion.runtime.annotations.builder.FluxtionIgnore;
import com.telamin.fluxtion.runtime.annotations.feature.Preview;
import com.telamin.fluxtion.runtime.annotations.runtime.ServiceDeregistered;
import com.telamin.fluxtion.runtime.annotations.runtime.ServiceRegistered;
import com.telamin.fluxtion.runtime.audit.Auditor;
import com.telamin.fluxtion.runtime.node.SingleNamedNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Manages service registrations and de-registrations pushing services into nodes that have methods annotated with:
 * {@code @ServiceRegistryNode}
 * {@code @ServiceDeregisteredNode}
 */
@Preview
public class ServiceRegistryNode
        extends SingleNamedNode
        implements
        Auditor,
        @ExportService(propagate = false) ServiceListener,
        // ServiceRegistryQuery is NOT exported — Fluxtion's @ExportService
        // mechanism only handles void-returning command methods. Callers
        // recover the query API via:
        //   ServiceRegistryQuery q = (ServiceRegistryQuery)
        //       dataFlow.getNodeById(ServiceRegistryNode.NODE_NAME);
        // This keeps the public contract on the interface (semver-stable)
        // while the node remains free to evolve.
        ServiceRegistryQuery {


    public static final String NODE_NAME = "serviceRegistry";
    @FluxtionIgnore
    private final Map<RegistrationKey, List<Callback>> serviceCallbackMap = new HashMap<>();
    @FluxtionIgnore
    private final Map<RegistrationKey, List<Callback>> serviceDeregisterCallbackMap = new HashMap<>();
    @FluxtionIgnore
    private final Map<Class<?>, List<Callback>> serviceWithNameCallbacks = new HashMap<>();
    @FluxtionIgnore
    private final Map<Class<?>, List<Callback>> serviceDeregisterWithNameCallbacks = new HashMap<>();

    // ── ServiceRegistryQuery cache ─────────────────────────────────────
    //
    // Query results are expensive to materialise (one ServiceDependency per
    // entry, each with a defensive copy of its consumer list) but the source
    // maps are only mutated during nodeRegistered() — typically at init time
    // and not on the hot path. Cache the built views and only rebuild when
    // a new node has been registered.
    //
    // The dirty flag is volatile so a read on any thread sees a recent
    // nodeRegistered() mutation; rebuilds are serialised on `cacheLock` so
    // concurrent first-readers don't duplicate work.
    @FluxtionIgnore
    private volatile boolean cacheDirty = true;
    @FluxtionIgnore
    private volatile List<ServiceDependency> cachedAll;
    @FluxtionIgnore
    private volatile Map<Class<?>, List<ServiceDependency>> cachedByClass;
    @FluxtionIgnore
    private volatile Map<RegistrationKey, ServiceDependency> cachedByKey;
    @FluxtionIgnore
    private final Object cacheLock = new Object();

    public ServiceRegistryNode() {
        super(NODE_NAME);
    }

    @Override
    public void registerService(Service<?> service) {
        auditLog.info("registerService", service);
        // Allocate the lookup key per call — a shared mutable temp would be
        // a data race when registerService runs concurrently with itself or
        // with ServiceRegistryQuery callers off the agent thread.
        RegistrationKey key = new RegistrationKey()
                .serviceClass(service.serviceClass())
                .serviceName(service.serviceName());
        List<Callback> callBackMethods = serviceCallbackMap.get(key);
        if (callBackMethods != null) {
            for (int i = 0; i < callBackMethods.size(); i++) {
                Callback callBackMethod = callBackMethods.get(i);
                callBackMethod.invoke(service.instance(), service.serviceName());
            }
        }

        for (Callback nameCallback : serviceWithNameCallbacks.getOrDefault(service.serviceClass(), Collections.emptyList())) {
            nameCallback.invoke(service.instance(), service.serviceName());
        }
    }

    @Override
    public void deRegisterService(Service<?> service) {
        auditLog.info("deRegisterService", service);
        RegistrationKey key = new RegistrationKey()
                .serviceClass(service.serviceClass())
                .serviceName(service.serviceName());
        List<Callback> callBackMethods = serviceDeregisterCallbackMap.get(key);
        if (callBackMethods != null) {
            for (int i = 0; i < callBackMethods.size(); i++) {
                Callback callBackMethod = callBackMethods.get(i);
                callBackMethod.invoke(service.instance(), service.serviceName());
            }
        }

        for (Callback nameCallback : serviceDeregisterWithNameCallbacks.getOrDefault(service.serviceClass(), Collections.emptyList())) {
            nameCallback.invoke(service.instance(), service.serviceName());
        }

    }

    @Override
    public void init() {
        serviceCallbackMap.clear();
        cacheDirty = true;
    }


    // ── ServiceRegistryQuery implementation ───────────────────────────
    //
    // Reads are cached behind a dirty flag: rebuild only when a new node has
    // been registered (which sets cacheDirty=true). Callers that hammer the
    // query interface (monitoring loops, UI polls) get amortised-zero-cost
    // reads after warmup.

    @Override
    public List<ServiceDependency> serviceDependencies() {
        ensureCacheFresh();
        return cachedAll;
    }

    @Override
    public List<ServiceDependency> serviceDependencies(Class<?> serviceClass) {
        java.util.Objects.requireNonNull(serviceClass, "serviceClass");
        ensureCacheFresh();
        List<ServiceDependency> hit = cachedByClass.get(serviceClass);
        return hit != null ? hit : Collections.emptyList();
    }

    @Override
    public java.util.Optional<ServiceDependency> findNamedDependency(Class<?> serviceClass, String serviceName) {
        java.util.Objects.requireNonNull(serviceClass, "serviceClass");
        java.util.Objects.requireNonNull(serviceName, "serviceName");
        ensureCacheFresh();
        RegistrationKey probe = new RegistrationKey().serviceClass(serviceClass).serviceName(serviceName);
        return java.util.Optional.ofNullable(cachedByKey.get(probe));
    }

    /**
     * Rebuild the query caches if a node has been registered since the last
     * read. Synchronised on a private lock so concurrent first-readers don't
     * each rebuild; cacheDirty is volatile so the unlocked fast path sees a
     * recent {@link #nodeRegistered} mutation promptly.
     */
    private void ensureCacheFresh() {
        if (!cacheDirty) return;
        synchronized (cacheLock) {
            if (!cacheDirty) return;
            List<ServiceDependency> all = new ArrayList<>();
            Map<Class<?>, List<ServiceDependency>> byClass = new HashMap<>();
            Map<RegistrationKey, ServiceDependency> byKey = new HashMap<>();
            for (Map.Entry<RegistrationKey, List<Callback>> e : serviceCallbackMap.entrySet()) {
                ServiceDependency d = buildDependency(
                        e.getKey().serviceClass(), e.getKey().serviceName(), e.getValue());
                all.add(d);
                byClass.computeIfAbsent(e.getKey().serviceClass(), k -> new ArrayList<>()).add(d);
                byKey.put(new RegistrationKey(e.getKey().serviceClass(), e.getKey().serviceName()), d);
            }
            for (Map.Entry<Class<?>, List<Callback>> e : serviceWithNameCallbacks.entrySet()) {
                ServiceDependency d = buildDependency(e.getKey(), null, e.getValue());
                all.add(d);
                byClass.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(d);
            }
            // Freeze the cached views so external mutators can't corrupt them.
            byClass.replaceAll((k, v) -> Collections.unmodifiableList(v));
            cachedAll = Collections.unmodifiableList(all);
            cachedByClass = Collections.unmodifiableMap(byClass);
            cachedByKey = byKey;     // internal, never handed out
            cacheDirty = false;
        }
    }

    /**
     * Snapshot a callback list into an immutable ServiceDependency.
     * Deduplicates by node identity — one node declaring multiple listener
     * methods for the same (serviceClass, serviceName) pair appears once
     * in the consumer list. Preserves first-encounter ordering.
     */
    private static ServiceDependency buildDependency(Class<?> cls, String name,
                                                     List<Callback> callbacks) {
        // IdentityHashMap-backed dedupe so a node with multiple matching
        // listener methods doesn't appear N times.
        java.util.LinkedHashMap<Integer, ServiceDependency.ConsumerNode> dedup =
                new java.util.LinkedHashMap<>();
        for (Callback c : callbacks) {
            dedup.putIfAbsent(System.identityHashCode(c.node()),
                    new ServiceDependency.ConsumerNode(c.node(), c.nodeName()));
        }
        return new ServiceDependency(cls, name, new ArrayList<>(dedup.values()));
    }

    @Override
    public void nodeRegistered(Object node, String nodeName) {
        if (node instanceof DataFlowContextListener) {
            ((DataFlowContextListener) node).currentContext(getDataFlowContext());
        }

        // Phase 1 — unlocked. Scan the node's methods (reflection-heavy) and
        // pre-build the callbacks. Most nodes have ZERO @ServiceRegistered
        // methods (event-handler-only nodes are the common case), so this
        // path usually produces an empty plan and never touches the lock.
        ScanResult plan = scanNode(node, nodeName);
        if (plan.isEmpty()) return;

        // Phase 2 — locked, narrow. Merge the plan into the live maps and
        // flip the dirty flag. Same lock the cache-rebuild uses, so a
        // concurrent reader can't see the maps mid-mutation. Skipping this
        // block when the plan is empty avoids spurious cache invalidation
        // on nodes that have no service listeners.
        synchronized (cacheLock) {
            for (CallbackEntry e : plan.registers) {
                if (e.byKey) {
                    serviceCallbackMap.computeIfAbsent(e.key, k -> new ArrayList<>()).add(e.callback);
                } else {
                    serviceWithNameCallbacks.computeIfAbsent(e.key.serviceClass(), k -> new ArrayList<>())
                            .add(e.callback);
                }
            }
            for (CallbackEntry e : plan.deregisters) {
                if (e.byKey) {
                    serviceDeregisterCallbackMap.computeIfAbsent(e.key, k -> new ArrayList<>()).add(e.callback);
                } else {
                    serviceDeregisterWithNameCallbacks.computeIfAbsent(e.key.serviceClass(), k -> new ArrayList<>())
                            .add(e.callback);
                }
            }
            // Only invalidate the query cache when register-side entries
            // were added; deregister-only entries don't affect the
            // dependency graph that getServiceDependencies() returns.
            if (!plan.registers.isEmpty()) {
                cacheDirty = true;
            }
        }
    }

    /**
     * Reflection scan — runs unlocked. Builds the list of (map, key, callback)
     * tuples to apply. Returns an empty plan if the node has no
     * {@code @ServiceRegistered} / {@code @ServiceDeregistered} listeners,
     * which is the common case for event-handler-only nodes.
     */
    private static ScanResult scanNode(Object node, String nodeName) {
        ScanResult out = new ScanResult();
        Class<?> clazz = node.getClass();
        for (Method method : clazz.getMethods()) {
            int parameterCount = method.getParameterCount();
            boolean namedService = parameterCount == 2
                    && CharSequence.class.isAssignableFrom(method.getParameterTypes()[1]);
            if (!Modifier.isPublic(method.getModifiers()) || (parameterCount != 1 && !namedService)) continue;
            Class<?> parameterType = method.getParameterTypes()[0];

            ServiceRegistered reg = method.getAnnotation(ServiceRegistered.class);
            if (reg != null) {
                RegistrationKey key = new RegistrationKey(
                        parameterType,
                        reg.value().isEmpty() ? parameterType.getCanonicalName() : reg.value());
                out.registers.add(new CallbackEntry(
                        key, new Callback(method, node, nodeName, namedService), !namedService));
            }
            ServiceDeregistered dereg = method.getAnnotation(ServiceDeregistered.class);
            if (dereg != null) {
                RegistrationKey key = new RegistrationKey(
                        parameterType,
                        dereg.value().isEmpty() ? parameterType.getCanonicalName() : dereg.value());
                out.deregisters.add(new CallbackEntry(
                        key, new Callback(method, node, nodeName, namedService), !namedService));
            }
        }
        return out;
    }

    private static final class CallbackEntry {
        final RegistrationKey key;
        final Callback callback;
        final boolean byKey;     // true for serviceCallbackMap, false for serviceWithNameCallbacks
        CallbackEntry(RegistrationKey key, Callback callback, boolean byKey) {
            this.key = key; this.callback = callback; this.byKey = byKey;
        }
    }

    private static final class ScanResult {
        final List<CallbackEntry> registers = new ArrayList<>();
        final List<CallbackEntry> deregisters = new ArrayList<>();
        boolean isEmpty() { return registers.isEmpty() && deregisters.isEmpty(); }
    }

    @Data
    @Accessors(chain = true, fluent = true)
    @AllArgsConstructor
    @NoArgsConstructor
    private static class RegistrationKey {
        Class<?> serviceClass;
        String serviceName;
    }

    @Data
    @Accessors(chain = true, fluent = true)
    @AllArgsConstructor
    @NoArgsConstructor
    private static class Callback {
        Method method;
        Object node;
        String nodeName;       // registered id of the node — fed through to ServiceDependency.ConsumerNode
        boolean namedService;

        @SneakyThrows
        void invoke(Object service, String name) {
            if (namedService) {
                method.invoke(node, service, name);
            } else {
                method.invoke(node, service);
            }
        }
    }
}
