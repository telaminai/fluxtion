/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.compile.config;

import com.telamin.fluxtion.builder.generation.config.EventProcessorConfig;
import com.telamin.fluxtion.runtime.annotations.builder.Disabled;

import java.io.File;

/**
 * A builder class for use with Fluxtion
 * <p>
 * Allows programmatic control of:
 * <ul>
 *     <li>Graph building using {@link #buildGraph(EventProcessorConfig)} method</li>
 *     <li>Generation using {@link #configureGeneration(FluxtionCompilerConfig)} method</li>
 * </ul>
 * <p>
 * Any builder marked with the {@link Disabled} annotation will be ignored
 */
public interface FluxtionGraphBuilder {
    void buildGraph(EventProcessorConfig eventProcessorConfig);

    void configureGeneration(FluxtionCompilerConfig compilerConfig);
}
