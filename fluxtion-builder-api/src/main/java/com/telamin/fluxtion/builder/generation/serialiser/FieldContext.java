/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.builder.generation.serialiser;

import com.telamin.fluxtion.builder.meta.model.Field;
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

    /**
     * The name of the field being serialised, or null where the caller did not supply one.
     *
     * <p>Java's serialisers never needed it: they return an EXPRESSION and the generator writes
     * {@code name = expression}. A target whose serialiser must produce a whole member DECLARATION
     * does need it — a C++ mapping for {@code Duration} emits
     * {@code std::chrono::nanoseconds horizon{...}}, and a serialiser handed only the value cannot
     * know the field is called {@code horizon}. Without it a serialiser had to invent a name, and two
     * fields of the same type in one node then collided.
     */
    private final String fieldName;

    public FieldContext(T instanceToMap, List<Field> nodeFields, Set<Class<?>> importList, MapFieldToJavaSource mapFieldToJavaSource) {
        this(instanceToMap, nodeFields, importList, mapFieldToJavaSource, null);
    }

    public FieldContext(T instanceToMap, List<Field> nodeFields, Set<Class<?>> importList, MapFieldToJavaSource mapFieldToJavaSource, String fieldName) {
        this.fieldName = fieldName;
        this.instanceToMap = instanceToMap;
        this.nodeFields = nodeFields;
        this.importList = importList;
        this.mapFieldToJavaSource = mapFieldToJavaSource;
    }

    public String mapToJavaSource(Object instance) {
        return mapFieldToJavaSource.mapToJavaSource(instance, nodeFields, importList);
    }

}
