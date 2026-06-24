/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.builder.generation.serialiser;

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
