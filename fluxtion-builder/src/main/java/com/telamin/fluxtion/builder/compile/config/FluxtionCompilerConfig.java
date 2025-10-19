/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.compile.config;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Configuration for the CloneableDataFlow compilation process.
 *
 * @author Greg Higgins
 */
@Getter
public class FluxtionCompilerConfig implements Serializable {

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
     * Flag controlling where the templated source file is written or the source is transient
     * <p>
     * not requires, default = true;
     */
    @Setter
    private boolean writeSourceToFile;

    /**
     * The if {@link #writeSourceToFile} is false this writer will capture the content of the generation process
     */
    private transient Writer sourceWriter;
    /**
     * Flag controlling adding build time to generated source files
     */
    @Setter
    private boolean addBuildTime;

    @Setter
    private transient ClassLoader classLoader;

    public FluxtionCompilerConfig() {
        generateDescription = false;
        writeSourceToFile = false;
        compileSource = true;
        addBuildTime = false;
        formatSource = true;
        classLoader = FluxtionCompilerConfig.class.getClassLoader();
        outputDirectory = OutputRegistry.JAVA_SRC_DIR;
        resourcesOutputDirectory = OutputRegistry.RESOURCE_DIR;
        sourceWriter = new StringWriter();
    }

    public String getFqn() {
        return getPackageName() + "." + getClassName();
    }

    public void setSourceWriter(Writer sourceWriter) {
        setFormatSource(true);
        setWriteSourceToFile(false);
        this.sourceWriter = sourceWriter;
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
                + ", compileSource=" + compileSource
                + ", interpreted=" + interpreted
                + ", formatSource=" + formatSource
                + ", templateSep=" + templateSep
                + ", generateDescription=" + generateDescription
                + '}';
    }

}
