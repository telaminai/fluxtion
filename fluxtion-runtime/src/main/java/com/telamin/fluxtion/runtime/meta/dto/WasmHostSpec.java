/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.dto;

import java.io.Serializable;

/**
 * Builder-side request for the metered WebAssembly host bundle. Carried on
 * {@link TopologicallySortedDependencyGraphDto}; the generator renders the actual {@code .java}
 * files from these names into {@code dto.wasmFiles} — the host shell is
 * pure templating (no graph analysis), and the {@code ReflectionSupplier} is rendered from the
 * graph's {@code @ServiceRegistered} analysis already carried in {@link ReachabilityMetadataDto}.
 *
 * <p>Populated only when {@code FluxtionCompilerConfig.generateWasmHost} is {@code true}; otherwise
 * the carrying field stays {@code null} and nothing WASM-related is emitted.
 *
 * <p>String-only and {@link Serializable} so it crosses the DTO wire boundary unchanged.
 */
public final class WasmHostSpec implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Package the host is emitted into — the generated SEP's own package. */
    private final String packageName;
    /** Simple class name of the generated SEP (e.g. {@code MyProcessor}). */
    private final String processorClassName;
    /** Simple class name of the emitted host (default {@code JsonHost}). */
    private final String hostClassName;

    public WasmHostSpec(String packageName, String processorClassName, String hostClassName) {
        this.packageName = packageName;
        this.processorClassName = processorClassName;
        this.hostClassName = hostClassName == null || hostClassName.trim().isEmpty() ? "JsonHost" : hostClassName;
    }

    public String getPackageName() { return packageName; }
    public String getProcessorClassName() { return processorClassName; }
    public String getHostClassName() { return hostClassName; }

    /** Fully-qualified name of the generated SEP. */
    public String processorFqn() {
        return packageName == null || packageName.isEmpty()
                ? processorClassName
                : packageName + "." + processorClassName;
    }
}
