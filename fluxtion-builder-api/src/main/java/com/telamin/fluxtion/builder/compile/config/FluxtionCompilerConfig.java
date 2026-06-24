/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.builder.compile.config;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Configuration for the CloneableDataFlow compilation process.
 * <p>
 * Standard JavaBean {@code void} setters are provided for framework compatibility (e.g. YAML/JSON binding).
 * Fluent builder-style methods (same name, returning {@code this}) are available for programmatic construction.
 *
 * @author Greg Higgins
 */
@Getter
public class FluxtionCompilerConfig implements Serializable {

    private static final String DEFAULT_JAVA_SOURCE_DIRECTORY =
            "target/generated-sources/fluxtion/";
    private static final String DEFAULT_RESOURCE_DIRECTORY =
            "src/main/resources/";

    /**
     * output package for generated SEP
     * <p>
     * required.
     */
    @Setter
    private String packageName;
    /**
     * class name for generated SEP
     * <p>
     * required.
     */
    @Setter
    private String className;
    /**
     * Output directory for generated SEP.
     * <p>
     * not required.
     */
    @Setter
    private String outputDirectory;
    /**
     * Output directory where compiled artifacts should be written. If null
     * no artifacts are written.
     */
    @Setter
    private String buildOutputDirectory;
    /**
     * Attempt to compile the generated source files
     */
    @Setter
    private boolean compileSource;
    /**
     * Generate an interpreted version
     */
    @Setter
    private boolean interpreted = false;
    /**
     * Generate a compiled version that uses the objects supplied as nodes in the processor. The dispatch table is
     * compiled
     */
    @Setter
    private boolean dispatchOnlyVersion = false;
    /**
     * Attempt to format the generated source files
     */
    @Setter
    private boolean formatSource;
    /**
     * Output for any resources generated with the SEP, such as debug information.
     * <p>
     * not required.
     */
    @Setter
    private String resourcesOutputDirectory;
    /**
     * The velocity template file to use in the SEP generation process. Default
     * value will be used if not supplied.
     * <p>
     * required.
     */
    @Setter
    private String templateSep;

    /**
     * Flag controlling generation of meta data description resources.
     * <p>
     * not required, default = true.
     */
    @Setter
    private boolean generateDescription;

    /**
     * When {@code true}, the builder analyses the graph and the compiler emits GraalVM native-image
     * reachability metadata to {@code META-INF/native-image/<processor-fqn>/reachability-metadata.json}
     * so an AOT processor's {@code @ServiceRegistered} service wiring (which the generated
     * {@code ServiceRegistryNode} resolves by runtime reflection) works in a native image.
     * <p>
     * Opt-in, mirroring {@link #generateDescription} (GraphML): most builds aren't native, so
     * default = false. See docs/native-reflection/README.md.
     */
    @Setter
    private boolean generateReachabilityMetadata;

    /**
     * When {@code true}, the builder emits a WebAssembly host shell alongside the generated SEP:
     * a fixed {@code @JSExport} class (default name {@link #wasmHostClassName} {@code = "JsonHost"})
     * that delegates every verb to the reusable {@code com.telamin.fluxtion.wasm.bootstrap.JsonBridgeHost},
     * plus its {@code @JSExportClasses} {@code <host>Main}. The two {@code .java} files are written into
     * the SEP's own source package, parameterised only by package + processor name — pure templating,
     * no graph analysis. They are emitted as <b>source</b> (not a resource), to be compiled by the
     * consumer's TeaVM build, which must have {@code fluxtion-wasm-bootstrap} + {@code teavm-jso} on
     * its classpath. The generator does not depend on those artifacts; it only writes the text.
     * <p>
     * Opt-in, mirroring {@link #generateReachabilityMetadata}: default = false.
     */
    @Setter
    private boolean generateWasmHost;

    /**
     * Simple class name of the emitted WASM host (see {@link #generateWasmHost}). The
     * {@code @JSExportClasses} entry point is emitted as {@code <wasmHostClassName>Main}.
     * <p>
     * Default {@code "JsonHost"}.
     */
    @Setter
    private String wasmHostClassName;

    /**
     * Flag controlling where the templated source file is written or the source is transient
     * <p>
     * not requires, default = true;
     */
    @Setter
    private boolean writeSourceToFile;

    /**
     * When {@code true}, the generated {@code .java} source is duplicated
     * into the {@link #resourcesOutputDirectory} at the same package path
     * so it rides into the packaged jar as a classpath resource alongside
     * the {@code .graphml}. Enables runtime tooling — the svc-admin-web
     * Processor-graph source viewer, the IntelliJ plugin's "jump to
     * generated source" action, audit replay UIs — to read the generated
     * source via the processor's own classloader, not just from the
     * build-tree filesystem.
     * <p>
     * Default {@code true}. Has no effect when {@link #writeSourceToFile}
     * is {@code false} (no source file exists to copy). Pair with
     * {@code <includes>**\/*.java</includes>} on
     * {@code maven-resources-plugin} if your build customises the default
     * resource filter.
     */
    @Setter
    private boolean copySourceToResourcesDirectory;

    /**
     * The if {@link #writeSourceToFile} is false this writer will capture the content of the generation process
     */
    private transient Writer sourceWriter;
    /**
     * Optional sink for the GraphML representation of the graph, produced
     * client-side by {@code TopologicallySortedDependencyGraphDtoBuilder}.
     * When non-null, generation populates this writer with the GraphML XML.
     * Mirrors {@link #sourceWriter} for source capture.
     * <p>
     * Default {@code null} — GraphML is not captured (unaltered legacy behaviour).
     */
    private transient Writer graphmlWriter;
    /**
     * If {@link #generateDescription} is true and {@link #graphmlWriter} is
     * null, write GraphML (and PNG) to disk — existing behaviour. When a
     * writer is supplied via {@link #setGraphmlWriter(Writer)}, this flag is
     * flipped to {@code false} so the file-write is skipped — useful in
     * environments without a writable resources directory (e.g. CheerpJ).
     * <p>
     * Default {@code true} — preserves legacy file-export behaviour.
     */
    @Setter
    private boolean writeGraphMlToFile = true;
    /**
     * Flag controlling adding build time to generated source files
     */
    @Setter
    private boolean addBuildTime;

    @Setter
    private transient ClassLoader classLoader;

    // ── Dispatch path merge settings ─────────────────────────────────────
    /**
     * Enable/disable dispatch path merging in generated source.
     * When enabled, common dispatch tails are extracted into shared helper methods.
     * <p>
     * Default {@code true}. Equivalent to system property {@code fluxtion.dispatch.mergeEnabled}.
     */
    @Setter
    private boolean dispatchMergeEnabled = true;
    /**
     * Minimum number of suffix operations required before a common dispatch tail is merged.
     * <p>
     * Default {@code 4}. Equivalent to system property {@code fluxtion.dispatch.minMergeOps}.
     */
    @Setter
    private int dispatchMinMergeOps = 4;
    /**
     * Minimum overlap percentage (0–100) required for a common dispatch tail to be merged.
     * <p>
     * Default {@code 40}. Equivalent to system property {@code fluxtion.dispatch.minMergeOverlapPct}.
     */
    @Setter
    private int dispatchMinMergeOverlapPct = 40;
    /**
     * When positive, a warning is printed to stderr if any dispatch chain exceeds this number of operations.
     * Useful for detecting methods that may approach the JVM 64KB bytecode limit.
     * <p>
     * Default {@code 0} (disabled). Equivalent to system property {@code fluxtion.dispatch.maxMethodOps}.
     */
    @Setter
    private int dispatchMaxMethodOps = 0;

    public FluxtionCompilerConfig() {
        generateDescription = false;
        writeSourceToFile = false;
        copySourceToResourcesDirectory = true;
        compileSource = true;
        addBuildTime = false;
        formatSource = false;
        classLoader = FluxtionCompilerConfig.class.getClassLoader();
        outputDirectory = DEFAULT_JAVA_SOURCE_DIRECTORY;
        resourcesOutputDirectory = DEFAULT_RESOURCE_DIRECTORY;
        sourceWriter = new StringWriter();
        generateWasmHost = false;
        wasmHostClassName = "JsonHost";
    }

    public String getFqn() {
        return getPackageName() + "." + getClassName();
    }

    public void setSourceWriter(Writer sourceWriter) {
        setFormatSource(true);
        setWriteSourceToFile(false);
        this.sourceWriter = sourceWriter;
    }

    /**
     * Sets the writer that will receive GraphML output. Passing a non-null
     * writer also flips {@link #generateDescription} to {@code true} (GraphML
     * is gated by that flag) and {@link #writeGraphMlToFile} to {@code false}
     * (route to the writer rather than the resources directory). Passing
     * {@code null} leaves both flags untouched, preserving any prior
     * configuration.
     */
    public void setGraphmlWriter(Writer graphmlWriter) {
        if (graphmlWriter != null) {
            setGenerateDescription(true);
            setWriteGraphMlToFile(false);
        }
        this.graphmlWriter = graphmlWriter;
    }

    // ── Fluent builder methods ──────────────────────────────────────────

    public FluxtionCompilerConfig dispatchMergeEnabled(boolean dispatchMergeEnabled) {
        this.dispatchMergeEnabled = dispatchMergeEnabled;
        return this;
    }

    public FluxtionCompilerConfig dispatchMinMergeOps(int dispatchMinMergeOps) {
        this.dispatchMinMergeOps = dispatchMinMergeOps;
        return this;
    }

    public FluxtionCompilerConfig dispatchMinMergeOverlapPct(int dispatchMinMergeOverlapPct) {
        this.dispatchMinMergeOverlapPct = dispatchMinMergeOverlapPct;
        return this;
    }

    public FluxtionCompilerConfig dispatchMaxMethodOps(int dispatchMaxMethodOps) {
        this.dispatchMaxMethodOps = dispatchMaxMethodOps;
        return this;
    }

    public FluxtionCompilerConfig packageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public FluxtionCompilerConfig className(String className) {
        this.className = className;
        return this;
    }

    public FluxtionCompilerConfig outputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }

    public FluxtionCompilerConfig buildOutputDirectory(String buildOutputDirectory) {
        this.buildOutputDirectory = buildOutputDirectory;
        return this;
    }

    public FluxtionCompilerConfig compileSource(boolean compileSource) {
        this.compileSource = compileSource;
        return this;
    }

    public FluxtionCompilerConfig interpreted(boolean interpreted) {
        this.interpreted = interpreted;
        return this;
    }

    public FluxtionCompilerConfig dispatchOnlyVersion(boolean dispatchOnlyVersion) {
        this.dispatchOnlyVersion = dispatchOnlyVersion;
        return this;
    }

    public FluxtionCompilerConfig formatSource(boolean formatSource) {
        this.formatSource = formatSource;
        return this;
    }

    public FluxtionCompilerConfig resourcesOutputDirectory(String resourcesOutputDirectory) {
        this.resourcesOutputDirectory = resourcesOutputDirectory;
        return this;
    }

    public FluxtionCompilerConfig templateSep(String templateSep) {
        this.templateSep = templateSep;
        return this;
    }

    public FluxtionCompilerConfig generateDescription(boolean generateDescription) {
        this.generateDescription = generateDescription;
        return this;
    }

    public FluxtionCompilerConfig generateReachabilityMetadata(boolean generateReachabilityMetadata) {
        this.generateReachabilityMetadata = generateReachabilityMetadata;
        return this;
    }

    public FluxtionCompilerConfig generateWasmHost(boolean generateWasmHost) {
        this.generateWasmHost = generateWasmHost;
        return this;
    }

    public FluxtionCompilerConfig wasmHostClassName(String wasmHostClassName) {
        this.wasmHostClassName = wasmHostClassName;
        return this;
    }

    public FluxtionCompilerConfig writeSourceToFile(boolean writeSourceToFile) {
        this.writeSourceToFile = writeSourceToFile;
        return this;
    }

    public FluxtionCompilerConfig copySourceToResourcesDirectory(boolean copySourceToResourcesDirectory) {
        this.copySourceToResourcesDirectory = copySourceToResourcesDirectory;
        return this;
    }

    public FluxtionCompilerConfig sourceWriter(Writer sourceWriter) {
        setSourceWriter(sourceWriter);
        return this;
    }

    public FluxtionCompilerConfig graphmlWriter(Writer graphmlWriter) {
        setGraphmlWriter(graphmlWriter);
        return this;
    }

    public FluxtionCompilerConfig writeGraphMlToFile(boolean writeGraphMlToFile) {
        this.writeGraphMlToFile = writeGraphMlToFile;
        return this;
    }

    public FluxtionCompilerConfig addBuildTime(boolean addBuildTime) {
        this.addBuildTime = addBuildTime;
        return this;
    }

    public FluxtionCompilerConfig classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    @Override
    public String toString() {
        return "SepCompilerConfig{"
                + "packageName=" + packageName
                + ", className=" + className
                + ", resourcesOutputDirectory=" + resourcesOutputDirectory
                + ", outputDirectory=" + outputDirectory
                + ", buildOutputdirectory=" + buildOutputDirectory
                + ", writeSourceToFile=" + writeSourceToFile
                + ", copySourceToResourcesDirectory=" + copySourceToResourcesDirectory
                + ", compileSource=" + compileSource
                + ", interpreted=" + interpreted
                + ", formatSource=" + formatSource
                + ", templateSep=" + templateSep
                + ", generateDescription=" + generateDescription
                + ", writeGraphMlToFile=" + writeGraphMlToFile
                + ", dispatchMergeEnabled=" + dispatchMergeEnabled
                + ", dispatchMinMergeOps=" + dispatchMinMergeOps
                + ", dispatchMinMergeOverlapPct=" + dispatchMinMergeOverlapPct
                + ", dispatchMaxMethodOps=" + dispatchMaxMethodOps
                + '}';
    }

}
