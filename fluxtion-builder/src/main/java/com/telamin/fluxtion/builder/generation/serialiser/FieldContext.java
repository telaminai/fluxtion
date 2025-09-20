/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.generation.serialiser;

import com.telamin.fluxtion.builder.generation.model.Field;
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
