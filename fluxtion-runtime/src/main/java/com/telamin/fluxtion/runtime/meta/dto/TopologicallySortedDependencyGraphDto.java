/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Serializable DTO representing everything the server-side
 * {@code SimpleEventProcessorModelGeneratorImpl} needs to generate a
 * {@code SimpleEventProcessorModel} without access to the live object graph or
 * {@code TopologicallySortedDependencyGraph}.
 *
 * <p>Populated on the client side by {@code TopologicallySortedDependencyGraphDtoBuilder}
 * after {@code TopologicallySortedDependencyGraph.generateDependencyTree()} has run.
 * Crosses the network boundary as-is; no live objects, no {@link java.lang.reflect.Method}
 * references, no class literals.
 */
public final class TopologicallySortedDependencyGraphDto implements Serializable {
    private static final long serialVersionUID = 3L;

    /**
     * All nodes in strict topological order (dependencies before dependents).
     * Preserves the order produced by {@code getObjectSortedDependents()}.
     */
    private final List<NodeDto> topologicallySortedNodes;

    /**
     * Auditor nodes keyed by auditor variable name. Value is an
     * {@link AuditorDto} carrying the canonical class name plus the
     * {@code auditInvocations} flag — the live auditor instance is not
     * available on the deserialise side, so the boolean is shipped
     * eagerly rather than re-derived from the instance.
     */
    private final Map<String, AuditorDto> auditorMap;

    /**
     * Exported service functions keyed by the interface {@link MethodDescriptor}.
     * Mirrors {@code TopologicallySortedDependencyGraph.getExportedFunctionMap()}.
     */
    private final Map<MethodDescriptor, ExportFunctionDataDto> exportedFunctionMap;

    // ── Config flags (from EventProcessorConfig) ─────────────────────────────

    /** Whether dirty-filtering support should be generated. */
    private final boolean supportDirtyFiltering;

    /** Whether buffer-and-trigger support should be generated. */
    private final boolean supportBufferAndTrigger;

    /** Whether this is a dispatch-only (no source generation) version. */
    private final boolean dispatchOnlyVersion;

    /**
     * Whether the full meta-model (including constructor strings, bean properties, graphML)
     * should be generated — {@code true} for source-generation paths, {@code false} for
     * in-memory (InMemoryEventProcessor) paths.
     */
    private final boolean forSourceGeneration;

    /**
     * Additional interfaces the generated event processor class must implement.
     * Stored as canonical class names.
     */
    private final List<String> interfacesToImplement;

    /**
     * Class name substitution overrides: original canonical class name → replacement canonical class name.
     * Mirrors {@code EventProcessorConfig.getClass2replace()}.
     */
    private final Map<String, String> class2replace;

    /**
     * Filter value overrides: filter string → list of event class names to apply the filter to.
     * Mirrors config filter overrides.
     */
    private final Map<String, List<String>> filterMap;

    /**
     * Node-level proxy class overrides: variable name → override canonical class name.
     * Mirrors {@code GenerationContext.getProxyClassMap()}.
     */
    private final Map<String, String> nodeClassOverrides;

    /**
     * Event-dispatch topological order: variable name → position in {@code topologicalHandlers}.
     * Used for sorting callback lists in the correct event-dispatch order (accounts for
     * {@code @PushReference} edges whose targets appear early in the noPush order but must
     * fire AFTER their event-graph parents).
     * Populated from {@code graph.getSortedDependents()} (= {@code topologicalHandlers}).
     */
    private final Map<String, Integer> eventDispatchPositionMap;

    /**
     * Pre-generated GraphML representation of the dependency graph.
     * Populated by {@code TopologicallySortedDependencyGraphDtoBuilder} when
     * {@code forSourceGeneration=true}. {@code null} for in-memory paths.
     */
    private String graphMlOutput;

    /**
     * Pre-generated PNG image bytes for the dependency graph.
     * Populated by {@code TopologicallySortedDependencyGraphDtoBuilder} when
     * {@code forSourceGeneration=true}. {@code null} for in-memory paths.
     */
    private byte[] pngOutput;

    /**
     * GraalVM native-image reachability metadata for the graph — the builder-side analysis
     * <em>input</em> (see {@code ReachabilityMetadataAnalyser}), populated when
     * {@code FluxtionCompilerConfig.generateReachabilityMetadata} is true; {@code null} otherwise.
     */
    private ReachabilityMetadataDto reachabilityMetadata;

    /**
     * Rendered {@code reachability-metadata.json} content — the generator (server/compiler) renders
     * {@link #reachabilityMetadata} to this string and the <em>client</em> writes it to
     * {@code META-INF/native-image/<fqn>/reachability-metadata.json}, exactly as generated source is
     * returned for the client to write/compile. {@code null} when the feature is off.
     */
    private String reachabilityMetadataJson;

    /**
     * Request for the metered WebAssembly host bundle — non-null when
     * {@code FluxtionCompilerConfig.generateWasmHost} is true. The (closed-source) generator reads
     * this and renders the bundle into {@link #wasmFiles}; {@code null} otherwise.
     */
    private WasmHostSpec wasmHostSpec;

    /**
     * Rendered WebAssembly host bundle — the generator (server/compiler) populates this from
     * {@link #wasmHostSpec} (and {@link #reachabilityMetadata} for the {@code ReflectionSupplier}),
     * and the <em>client</em> writes each entry to disk. Keys are output names: a bare {@code *.java}
     * name lands in the SEP's source package; any other key is a resources-root-relative path (e.g.
     * {@code META-INF/services/org.teavm.classlib.ReflectionSupplier}). {@code null} when the bundle
     * is not requested or a generation backend does not return it.
     */
    private Map<String, String> wasmFiles;

    /**
     * Generic, <em>ungated</em> extension point for any future generated output (name → content),
     * mirroring {@link #wasmFiles} but with no metering gate — a generator stage just puts entries
     * here, the server returns them and the client path-routes + writes them, so adding a new
     * generated artifact is a one-stage change rather than threading a new field through DTO →
     * response → server → client. Write routing (client side): a {@code *.java} key with a path
     * (e.g. {@code com/acme/Foo.java}) → source root; a bare {@code *.java} name → the SEP package;
     * any other key → resources root. Currently unused — reserved infrastructure. {@code null} when
     * empty. (The WASM bundle keeps its own {@link #wasmFiles} field precisely because it IS gated.)
     */
    private Map<String, String> generatedArtifacts;
    /** Import class canonical names collected by FieldSerializer during source-gen DTO building. */
    private Set<String> importClassStrings = new HashSet<>();

    /**
     * Sorted hierarchy of every event class seen in the graph.
     * Key   = canonical class name (TreeMap keeps keys in natural/alphabetical order).
     * Value = ancestor chain from most-specific to least-specific:
     *         [className, directSuperclass, ..., "java.lang.Object", interfaces…].
     *
     * <p>Populated on the client side (where live {@link Class} objects are available)
     * by {@code TopologicallySortedDependencyGraphDtoBuilder}. On the server side this
     * map provides a simple string-only lookup:
     * <ul>
     *   <li>Is {@code E} assignable to {@code H}? → check {@code hierarchy.get(E).contains(H)}</li>
     *   <li>Most specialised handler? → lowest index in {@code hierarchy.get(E)}</li>
     * </ul>
     */
    private TreeMap<String, List<String>> eventClassHierarchy = new TreeMap<>();

    private TopologicallySortedDependencyGraphDto(Builder b) {
        this.topologicallySortedNodes = Collections.unmodifiableList(new ArrayList<>(b.topologicallySortedNodes));
        this.auditorMap = Collections.unmodifiableMap(new LinkedHashMap<>(b.auditorMap));
        this.exportedFunctionMap = Collections.unmodifiableMap(new LinkedHashMap<>(b.exportedFunctionMap));
        this.supportDirtyFiltering = b.supportDirtyFiltering;
        this.supportBufferAndTrigger = b.supportBufferAndTrigger;
        this.dispatchOnlyVersion = b.dispatchOnlyVersion;
        this.forSourceGeneration = b.forSourceGeneration;
        this.interfacesToImplement = Collections.unmodifiableList(new ArrayList<>(b.interfacesToImplement));
        this.class2replace = Collections.unmodifiableMap(new HashMap<>(b.class2replace));
        this.filterMap = Collections.unmodifiableMap(new HashMap<>(b.filterMap));
        this.nodeClassOverrides = Collections.unmodifiableMap(new HashMap<>(b.nodeClassOverrides));
        this.eventDispatchPositionMap = Collections.unmodifiableMap(new HashMap<>(b.eventDispatchPositionMap));
    }

    public List<NodeDto> getTopologicallySortedNodes() { return topologicallySortedNodes; }
    public Map<String, AuditorDto> getAuditorMap() { return auditorMap; }
    public Map<MethodDescriptor, ExportFunctionDataDto> getExportedFunctionMap() { return exportedFunctionMap; }
    public boolean isSupportDirtyFiltering() { return supportDirtyFiltering; }
    public boolean isSupportBufferAndTrigger() { return supportBufferAndTrigger; }
    public boolean isDispatchOnlyVersion() { return dispatchOnlyVersion; }
    public boolean isForSourceGeneration() { return forSourceGeneration; }
    public List<String> getInterfacesToImplement() { return interfacesToImplement; }
    public Map<String, String> getClass2replace() { return class2replace; }
    public Map<String, List<String>> getFilterMap() { return filterMap; }
    public Map<String, String> getNodeClassOverrides() { return nodeClassOverrides; }
    public Map<String, Integer> getEventDispatchPositionMap() { return eventDispatchPositionMap; }
    public String getGraphMlOutput() { return graphMlOutput; }
    public void setGraphMlOutput(String graphMlOutput) { this.graphMlOutput = graphMlOutput; }
    public byte[] getPngOutput() { return pngOutput; }
    public void setPngOutput(byte[] pngOutput) { this.pngOutput = pngOutput; }
    public ReachabilityMetadataDto getReachabilityMetadata() { return reachabilityMetadata; }
    public void setReachabilityMetadata(ReachabilityMetadataDto reachabilityMetadata) { this.reachabilityMetadata = reachabilityMetadata; }
    public String getReachabilityMetadataJson() { return reachabilityMetadataJson; }
    public void setReachabilityMetadataJson(String reachabilityMetadataJson) { this.reachabilityMetadataJson = reachabilityMetadataJson; }
    public WasmHostSpec getWasmHostSpec() { return wasmHostSpec; }
    public void setWasmHostSpec(WasmHostSpec wasmHostSpec) { this.wasmHostSpec = wasmHostSpec; }
    public Map<String, String> getWasmFiles() { return wasmFiles; }
    public void setWasmFiles(Map<String, String> wasmFiles) { this.wasmFiles = wasmFiles; }
    public Map<String, String> getGeneratedArtifacts() { return generatedArtifacts; }
    public void setGeneratedArtifacts(Map<String, String> generatedArtifacts) { this.generatedArtifacts = generatedArtifacts; }
    public Set<String> getImportClassStrings() { return importClassStrings; }
    public void setImportClassStrings(Set<String> importClassStrings) { this.importClassStrings = importClassStrings; }
    public TreeMap<String, List<String>> getEventClassHierarchy() { return eventClassHierarchy; }
    public void setEventClassHierarchy(TreeMap<String, List<String>> eventClassHierarchy) { this.eventClassHierarchy = eventClassHierarchy; }

    /** Convenience: find a node by variable name, or {@code null}. */
    public NodeDto findNode(String variableName) {
        for (NodeDto n : topologicallySortedNodes) {
            if (n.getVariableName().equals(variableName)) return n;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TopologicallySortedDependencyGraphDto)) return false;
        TopologicallySortedDependencyGraphDto that = (TopologicallySortedDependencyGraphDto) o;
        return supportDirtyFiltering == that.supportDirtyFiltering
                && supportBufferAndTrigger == that.supportBufferAndTrigger
                && dispatchOnlyVersion == that.dispatchOnlyVersion
                && forSourceGeneration == that.forSourceGeneration
                && Objects.equals(topologicallySortedNodes, that.topologicallySortedNodes)
                && Objects.equals(auditorMap, that.auditorMap)
                && Objects.equals(exportedFunctionMap, that.exportedFunctionMap)
                && Objects.equals(interfacesToImplement, that.interfacesToImplement)
                && Objects.equals(class2replace, that.class2replace)
                && Objects.equals(filterMap, that.filterMap)
                && Objects.equals(nodeClassOverrides, that.nodeClassOverrides);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topologicallySortedNodes, auditorMap, exportedFunctionMap);
    }

    @Override
    public String toString() {
        return "TopologicallySortedDependencyGraphDto{nodes=" + topologicallySortedNodes.size()
                + ", auditors=" + auditorMap.keySet()
                + ", exported=" + exportedFunctionMap.size() + "}";
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private List<NodeDto> topologicallySortedNodes = new ArrayList<>();
        private Map<String, AuditorDto> auditorMap = new LinkedHashMap<>();
        private Map<MethodDescriptor, ExportFunctionDataDto> exportedFunctionMap = new LinkedHashMap<>();
        private boolean supportDirtyFiltering;
        private boolean supportBufferAndTrigger;
        private boolean dispatchOnlyVersion;
        private boolean forSourceGeneration;
        private List<String> interfacesToImplement = new ArrayList<>();
        private Map<String, String> class2replace = new HashMap<>();
        private Map<String, List<String>> filterMap = new HashMap<>();
        private Map<String, String> nodeClassOverrides = new HashMap<>();
        private Map<String, Integer> eventDispatchPositionMap = new HashMap<>();

        private Builder() {}

        public Builder topologicallySortedNodes(List<NodeDto> nodes) { this.topologicallySortedNodes = nodes; return this; }
        public Builder addNode(NodeDto node) { this.topologicallySortedNodes.add(node); return this; }
        public Builder auditorMap(Map<String, AuditorDto> m) { this.auditorMap = m; return this; }
        public Builder addAuditor(String name, AuditorDto auditorDto) { this.auditorMap.put(name, auditorDto); return this; }
        public Builder exportedFunctionMap(Map<MethodDescriptor, ExportFunctionDataDto> m) { this.exportedFunctionMap = m; return this; }
        public Builder addExportedFunction(MethodDescriptor md, ExportFunctionDataDto d) { this.exportedFunctionMap.put(md, d); return this; }
        public Builder supportDirtyFiltering(boolean v) { this.supportDirtyFiltering = v; return this; }
        public Builder supportBufferAndTrigger(boolean v) { this.supportBufferAndTrigger = v; return this; }
        public Builder dispatchOnlyVersion(boolean v) { this.dispatchOnlyVersion = v; return this; }
        public Builder forSourceGeneration(boolean v) { this.forSourceGeneration = v; return this; }
        public Builder interfacesToImplement(List<String> v) { this.interfacesToImplement = v; return this; }
        public Builder class2replace(Map<String, String> v) { this.class2replace = v; return this; }
        public Builder filterMap(Map<String, List<String>> v) { this.filterMap = v; return this; }
        public Builder nodeClassOverrides(Map<String, String> v) { this.nodeClassOverrides = v; return this; }
        public Builder eventDispatchPositionMap(Map<String, Integer> v) { this.eventDispatchPositionMap = v; return this; }

        public TopologicallySortedDependencyGraphDto build() {
            return new TopologicallySortedDependencyGraphDto(this);
        }
    }
}
