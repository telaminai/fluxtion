/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.generation.serialiser;

import com.fluxtion.dataflow.builder.generation.model.Field;

import java.util.List;
import java.util.Set;

public interface MapFieldToJavaSource {
    String mapToJavaSource(Object primitiveVal, List<Field> nodeFields, Set<Class<?>> importList);
}
