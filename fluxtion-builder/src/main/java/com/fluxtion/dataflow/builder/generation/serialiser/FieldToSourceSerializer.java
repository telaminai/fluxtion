/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.generation.serialiser;

import java.util.ServiceLoader;

/**
 * Loads a FieldToSourceSerializer using the {@link ServiceLoader} support provided
 * by Java platform. New factories can be added to Fluxtion using the extension
 * mechanism described in {@link ServiceLoader} documentation.
 */
public interface FieldToSourceSerializer<T> {

    int DEFAULT_PRIORITY = 500;

    boolean typeSupported(Class<?> type);

    String mapToSource(FieldContext<T> fieldContext);

    default String language() {
        return "java";
    }

    default int priority() {
        return DEFAULT_PRIORITY;
    }
}
