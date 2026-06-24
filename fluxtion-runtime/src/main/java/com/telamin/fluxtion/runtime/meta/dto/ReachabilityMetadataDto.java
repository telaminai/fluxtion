/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder-side analysis result for GraalVM native-image reachability metadata.
 *
 * <p>A Fluxtion AOT processor dispatches statically (reflection-free), but the generated
 * {@code ServiceRegistryNode} still wires {@code @ServiceRegistered}/{@code @ServiceDeregistered}
 * services by runtime reflection ({@code getClass().getMethods()} → {@code getAnnotation} →
 * {@code invoke}). In a native image those methods are invisible unless registered, so service
 * injection silently no-ops. The builder walks the whole graph (user <em>and</em> framework nodes)
 * and records exactly the methods that need registering — the compiler renders this to
 * {@code META-INF/native-image/<processor-fqn>/reachability-metadata.json}.
 *
 * <p>String-only (FQNs + method names) — serialisable, crosses the DTO boundary with no live
 * {@link java.lang.reflect.Method} or class literals. Populated only when
 * {@code FluxtionCompilerConfig.generateReachabilityMetadata} is {@code true}; otherwise the
 * carrying field on {@link TopologicallySortedDependencyGraphDto} stays {@code null}.
 *
 * @see <a href="...">docs/native-reflection/README.md</a>
 */
public final class ReachabilityMetadataDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * A single reflectively-invoked method that must be registered for reflective query + invoke.
     * {@code declaringType} is the concrete node class the runtime calls {@code getMethods()} on
     * (matches the GraalVM tracing-agent capture, which keys on the queried type).
     */
    public static final class ReflectiveMethod implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String declaringType;
        private final String name;
        private final List<String> parameterTypes;

        public ReflectiveMethod(String declaringType, String name, List<String> parameterTypes) {
            this.declaringType = declaringType;
            this.name = name;
            this.parameterTypes = parameterTypes == null ? new ArrayList<>() : parameterTypes;
        }

        public String getDeclaringType() { return declaringType; }
        public String getName() { return name; }
        public List<String> getParameterTypes() { return parameterTypes; }
    }

    /**
     * FQN of the generated processor, registered for reflective {@code <init>} so a {@code Main}
     * that loads it via {@code Class.forName(...).newInstance()} (the bare-AOT shape) works in
     * native. Harmless when the host instantiates it directly ({@code new Processor()}).
     */
    private final String processorClassName;

    /**
     * Every {@code @ServiceRegistered}/{@code @ServiceDeregistered} method across all graph nodes
     * (user and framework). Minimal: only the annotated methods, not whole-class registration.
     */
    private final List<ReflectiveMethod> serviceMethods;

    public ReachabilityMetadataDto(String processorClassName, List<ReflectiveMethod> serviceMethods) {
        this.processorClassName = processorClassName;
        this.serviceMethods = serviceMethods == null ? new ArrayList<>() : serviceMethods;
    }

    public String getProcessorClassName() { return processorClassName; }
    public List<ReflectiveMethod> getServiceMethods() { return serviceMethods; }

    public boolean isEmpty() {
        return serviceMethods.isEmpty() && (processorClassName == null || processorClassName.isEmpty());
    }
}
