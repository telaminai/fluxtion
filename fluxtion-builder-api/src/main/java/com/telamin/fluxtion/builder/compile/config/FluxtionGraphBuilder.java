/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
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
