/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.builder.generation.serialiser;

import com.telamin.fluxtion.builder.meta.model.Field;

import java.util.List;
import java.util.Set;

public interface MapFieldToJavaSource {
    String mapToJavaSource(Object primitiveVal, List<Field> nodeFields, Set<Class<?>> importList);

    String mapToJavaConstructorSource(Object primitiveVal, List<Field> nodeFields, Set<Class<?>> importList);
}
