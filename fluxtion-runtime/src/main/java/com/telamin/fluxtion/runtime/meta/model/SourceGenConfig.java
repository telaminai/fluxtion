/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration DTO that carries all source-generation parameters needed by the server-side
 * generator. This replaces the need for the server to access EventProcessorConfig,
 * FluxtionCompilerConfig, or GenerationContext from the builder.
 */
public class SourceGenConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    // dispatch / code-gen strategy
    private DispatchStrategy dispatchStrategy = DispatchStrategy.PATTERN_MATCH;
    private boolean inlineEventHandling = true;
    private boolean supportBufferAndTrigger = false;

    // naming context (previously from GenerationContext)
    private String packageName;
    private String className;

    // template (previously from EventProcessorConfig)
    private String templateFile;
    private Set<ClassName> interfacesToImplement = new HashSet<>();

    // version information
    private String generatorVersion;
    private String apiVersion;

    // compiler config fields (previously from FluxtionCompilerConfig)
    private boolean addBuildTime;
    private String outputDirectory;
    private String resourcesOutputDirectory;
    private String buildOutputDirectory;
    private boolean writeSourceToFile;
    private boolean compileSource = true;
    private boolean formatSource = true;
    private boolean generateDescription;

    // transient classloader for local use
    private transient ClassLoader classLoader;

    // dispatch path merge settings
    private boolean dispatchMergeEnabled = true;
    private int dispatchMinMergeOps = 4;
    private int dispatchMinMergeOverlapPct = 40;
    private int dispatchMaxMethodOps = 0;

    public SourceGenConfig() {
    }

    // --- Getters and setters ---

    public DispatchStrategy getDispatchStrategy() { return dispatchStrategy; }
    public void setDispatchStrategy(DispatchStrategy dispatchStrategy) { this.dispatchStrategy = dispatchStrategy; }

    public boolean isInlineEventHandling() { return inlineEventHandling; }
    public void setInlineEventHandling(boolean inlineEventHandling) { this.inlineEventHandling = inlineEventHandling; }

    public boolean isSupportBufferAndTrigger() { return supportBufferAndTrigger; }
    public void setSupportBufferAndTrigger(boolean supportBufferAndTrigger) { this.supportBufferAndTrigger = supportBufferAndTrigger; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getTemplateFile() { return templateFile; }
    public void setTemplateFile(String templateFile) { this.templateFile = templateFile; }

    public Set<ClassName> getInterfacesToImplement() { return interfacesToImplement; }
    public void setInterfacesToImplement(Set<ClassName> interfacesToImplement) { this.interfacesToImplement = interfacesToImplement; }

    public String getGeneratorVersion() { return generatorVersion; }
    public void setGeneratorVersion(String generatorVersion) { this.generatorVersion = generatorVersion; }

    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }

    public boolean isAddBuildTime() { return addBuildTime; }
    public void setAddBuildTime(boolean addBuildTime) { this.addBuildTime = addBuildTime; }

    public String getOutputDirectory() { return outputDirectory; }
    public void setOutputDirectory(String outputDirectory) { this.outputDirectory = outputDirectory; }

    public String getResourcesOutputDirectory() { return resourcesOutputDirectory; }
    public void setResourcesOutputDirectory(String resourcesOutputDirectory) { this.resourcesOutputDirectory = resourcesOutputDirectory; }

    public String getBuildOutputDirectory() { return buildOutputDirectory; }
    public void setBuildOutputDirectory(String buildOutputDirectory) { this.buildOutputDirectory = buildOutputDirectory; }

    public boolean isWriteSourceToFile() { return writeSourceToFile; }
    public void setWriteSourceToFile(boolean writeSourceToFile) { this.writeSourceToFile = writeSourceToFile; }

    public boolean isCompileSource() { return compileSource; }
    public void setCompileSource(boolean compileSource) { this.compileSource = compileSource; }

    public boolean isFormatSource() { return formatSource; }
    public void setFormatSource(boolean formatSource) { this.formatSource = formatSource; }

    public boolean isGenerateDescription() { return generateDescription; }
    public void setGenerateDescription(boolean generateDescription) { this.generateDescription = generateDescription; }

    public ClassLoader getClassLoader() { return classLoader; }
    public void setClassLoader(ClassLoader classLoader) { this.classLoader = classLoader; }

    public boolean isDispatchMergeEnabled() { return dispatchMergeEnabled; }
    public void setDispatchMergeEnabled(boolean dispatchMergeEnabled) { this.dispatchMergeEnabled = dispatchMergeEnabled; }

    public int getDispatchMinMergeOps() { return dispatchMinMergeOps; }
    public void setDispatchMinMergeOps(int dispatchMinMergeOps) { this.dispatchMinMergeOps = dispatchMinMergeOps; }

    public int getDispatchMinMergeOverlapPct() { return dispatchMinMergeOverlapPct; }
    public void setDispatchMinMergeOverlapPct(int dispatchMinMergeOverlapPct) { this.dispatchMinMergeOverlapPct = dispatchMinMergeOverlapPct; }

    public int getDispatchMaxMethodOps() { return dispatchMaxMethodOps; }
    public void setDispatchMaxMethodOps(int dispatchMaxMethodOps) { this.dispatchMaxMethodOps = dispatchMaxMethodOps; }

    public boolean isPatternDispatch() {
        return dispatchStrategy == DispatchStrategy.PATTERN_MATCH;
    }

    public boolean isInstanceOfDispatch() {
        return dispatchStrategy == DispatchStrategy.INSTANCE_OF;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceGenConfig that = (SourceGenConfig) o;
        return inlineEventHandling == that.inlineEventHandling &&
                supportBufferAndTrigger == that.supportBufferAndTrigger &&
                addBuildTime == that.addBuildTime &&
                writeSourceToFile == that.writeSourceToFile &&
                compileSource == that.compileSource &&
                formatSource == that.formatSource &&
                generateDescription == that.generateDescription &&
                dispatchStrategy == that.dispatchStrategy &&
                Objects.equals(packageName, that.packageName) &&
                Objects.equals(className, that.className) &&
                Objects.equals(templateFile, that.templateFile) &&
                Objects.equals(interfacesToImplement, that.interfacesToImplement) &&
                Objects.equals(generatorVersion, that.generatorVersion) &&
                Objects.equals(apiVersion, that.apiVersion) &&
                Objects.equals(outputDirectory, that.outputDirectory) &&
                Objects.equals(resourcesOutputDirectory, that.resourcesOutputDirectory) &&
                Objects.equals(buildOutputDirectory, that.buildOutputDirectory) &&
                dispatchMergeEnabled == that.dispatchMergeEnabled &&
                dispatchMinMergeOps == that.dispatchMinMergeOps &&
                dispatchMinMergeOverlapPct == that.dispatchMinMergeOverlapPct &&
                dispatchMaxMethodOps == that.dispatchMaxMethodOps;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dispatchStrategy, inlineEventHandling, supportBufferAndTrigger, packageName, className,
                templateFile, interfacesToImplement, generatorVersion, apiVersion, addBuildTime, outputDirectory,
                resourcesOutputDirectory, buildOutputDirectory, writeSourceToFile, compileSource, formatSource,
                generateDescription, dispatchMergeEnabled, dispatchMinMergeOps, dispatchMinMergeOverlapPct,
                dispatchMaxMethodOps);
    }

    @Override
    public String toString() {
        return "SourceGenConfig{" +
                "dispatchStrategy=" + dispatchStrategy +
                ", inlineEventHandling=" + inlineEventHandling +
                ", supportBufferAndTrigger=" + supportBufferAndTrigger +
                ", packageName='" + packageName + '\'' +
                ", className='" + className + '\'' +
                ", templateFile='" + templateFile + '\'' +
                ", addBuildTime=" + addBuildTime +
                '}';
    }
}
