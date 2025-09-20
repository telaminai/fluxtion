/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.generation.serialiser;

import com.fluxtion.dataflow.builder.generation.model.Field;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Getter
public class FieldContext<T> {
    private final T instanceToMap;
    private final List<Field> nodeFields;
    private final Set<Class<?>> importList;
    @Getter(AccessLevel.NONE)
    private final MapFieldToJavaSource mapFieldToJavaSource;

    public FieldContext(T instanceToMap, List<Field> nodeFields, Set<Class<?>> importList, MapFieldToJavaSource mapFieldToJavaSource) {
        this.instanceToMap = instanceToMap;
        this.nodeFields = nodeFields;
        this.importList = importList;
        this.mapFieldToJavaSource = mapFieldToJavaSource;
    }

    public String mapToJavaSource(Object instance) {
        return mapFieldToJavaSource.mapToJavaSource(instance, nodeFields, importList);
    }

}
