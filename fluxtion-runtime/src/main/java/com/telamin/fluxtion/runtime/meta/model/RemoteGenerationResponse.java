/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.model;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public class RemoteGenerationResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String source;
    private String error;
    /**
     * Rendered GraalVM {@code reachability-metadata.json} for the generated processor, or {@code null}
     * when the build didn't request it. The generator copies this from
     * {@code dto.getReachabilityMetadataJson()} after rendering; the client writes it to disk. Added as
     * a nullable field so an older server (which omits it) deserialises cleanly to {@code null}.
     */
    private String reachabilityMetadataJson;

    /**
     * Optional WebAssembly host bundle (host shell + {@code @JSExportClasses} main, plus a
     * {@code ReflectionSupplier} + SPI resource when the SEP uses {@code @ServiceRegistered}), keyed
     * by output name — see {@code TopologicallySortedDependencyGraphDto.wasmFiles}. {@code null} when
     * the build did not request it or a generation backend does not return it. Nullable so an older
     * server deserialises cleanly to {@code null}.
     */
    private Map<String, String> wasmFiles;

    /**
     * Generic, ungated extension point mirroring
     * {@code TopologicallySortedDependencyGraphDto.generatedArtifacts}: any future generated output
     * (name → content) the server returns and the client writes, so a new artifact type needs no new
     * response field. Distinct from {@link #wasmFiles}, which is the metered/gated channel. Nullable
     * for back-compat; currently unused — reserved infrastructure.
     */
    private Map<String, String> generatedArtifacts;

    public static RemoteGenerationResponse ok(String source) {
        RemoteGenerationResponse r = new RemoteGenerationResponse();
        r.success = true;
        r.source = source;
        return r;
    }

    public static RemoteGenerationResponse ok(String source, String reachabilityMetadataJson) {
        RemoteGenerationResponse r = ok(source);
        r.reachabilityMetadataJson = reachabilityMetadataJson;
        return r;
    }

    public static RemoteGenerationResponse ok(String source, String reachabilityMetadataJson,
                                              Map<String, String> wasmFiles) {
        RemoteGenerationResponse r = ok(source, reachabilityMetadataJson);
        r.wasmFiles = wasmFiles;
        return r;
    }

    public static RemoteGenerationResponse error(String msg) {
        RemoteGenerationResponse r = new RemoteGenerationResponse();
        r.success = false;
        r.error = msg;
        return r;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getReachabilityMetadataJson() { return reachabilityMetadataJson; }
    public void setReachabilityMetadataJson(String reachabilityMetadataJson) { this.reachabilityMetadataJson = reachabilityMetadataJson; }

    public Map<String, String> getWasmFiles() { return wasmFiles; }
    public void setWasmFiles(Map<String, String> wasmFiles) { this.wasmFiles = wasmFiles; }

    public Map<String, String> getGeneratedArtifacts() { return generatedArtifacts; }
    public void setGeneratedArtifacts(Map<String, String> generatedArtifacts) { this.generatedArtifacts = generatedArtifacts; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteGenerationResponse that = (RemoteGenerationResponse) o;
        return success == that.success && Objects.equals(source, that.source) && Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, source, error);
    }

    @Override
    public String toString() {
        return "RemoteGenerationResponse{" +
                "success=" + success +
                ", source='" + source + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}
