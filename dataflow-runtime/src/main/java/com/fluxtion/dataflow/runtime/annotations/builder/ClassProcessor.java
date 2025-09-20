/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.annotations.builder;

import java.io.File;
import java.net.URL;
import java.util.ServiceLoader;

/**
 * A ClassProcessor service can inspect and process application classes after
 * they are compiled. The callback {@link #process(URL) } points to the
 * compiled application classes. No external libraries are on the process URL,
 * solely the output of compiling application source files.<p>
 * <p>
 * This gives the opportunity for {@link ClassProcessor}'s to scan the path and
 * generate artifacts without risk of confusing library and application classes.
 * For example a service may scan for a specific annotation and generate a
 * tailored solution based on the meta-data discovered during scanning.<p>
 *
 * <h2>Registering ClassProcessor</h2>
 * Fluxtion employs the {@link ServiceLoader} pattern to register user
 * implemented NodeFactories. Please read the java documentation describing the
 * meta-data a node implementor must provide to register a node using the
 * {@link ServiceLoader} pattern.
 *
 * @author 2024 gregory higgins.
 */
public interface ClassProcessor {

    /**
     * Directories for the current generation context
     *
     * @param rootDir     - root directory of the project
     * @param output      - directory for generated source outputs
     * @param resourceDir - directory for generated resource outputs
     */
    default void outputDirectories(File rootDir, File output, File resourceDir) {

    }

    /**
     * The URL of compiled application classes
     *
     * @param classPath application classes location
     */
    void process(URL classPath);
}
