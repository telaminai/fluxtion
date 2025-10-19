/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.compile.serialiser;

import com.telamin.fluxtion.builder.generation.model.Field;
import com.telamin.fluxtion.builder.generation.serialiser.FieldContext;
import com.telamin.fluxtion.runtime.serializer.MapBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public interface CollectionSerializer {

    static <T extends Map<?, ?>>String mapToSource(FieldContext<T> fieldContext) {
        fieldContext.getImportList().add(MapBuilder.class);
        Map<?, ?> values = fieldContext.getInstanceToMap();
        Map<Object, Object> newMap = new HashMap<>();
        values.entrySet().stream().forEach(item -> {
            Object key = item.getKey();
            Object value = item.getValue();
            for (Field nodeField : fieldContext.getNodeFields()) {
                if (nodeField.getInstance().equals(key)) {
                    key = nodeField.getInstance();
                    break;
                }
            }
            for (Field nodeField : fieldContext.getNodeFields()) {
                if (nodeField.getInstance().equals(value)) {
                    value = nodeField.getInstance();
                    break;
                }
            }
            newMap.put(key, value);
        });

        if(newMap.isEmpty()){
            fieldContext.getImportList().add(MapBuilder.class);
            return "MapBuilder.builder().build()";
        }

        return newMap.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().toString())).map(
                entry -> {
                    String keyString = fieldContext.mapToJavaSource(entry.getKey());
                    String valueString = fieldContext.mapToJavaSource(entry.getValue());
                    return "(" + keyString + ", " + valueString + ")";
                }
        ).collect(Collectors.joining(".put", "MapBuilder.builder().put", ".build()"));
    }

    static String hashMapToSource(FieldContext<HashMap<?, ?>> fieldContext){
        fieldContext.getImportList().add(HashMap.class);
        String map = mapToSource(fieldContext);
        return "new HashMap<>(" + map + ")";
    }

    @NotNull
    static String setToSource(FieldContext<Set<?>> fieldContext) {
        fieldContext.getImportList().add(Arrays.class);
        fieldContext.getImportList().add(HashSet.class);
        Set<?> values = fieldContext.getInstanceToMap();
        Set<Object> newList = new HashSet<>();
        values.stream().forEach(item -> {
            boolean foundMatch = false;
            for (Field nodeField : fieldContext.getNodeFields()) {
                if (nodeField.getInstance().equals(item)) {
                    newList.add(nodeField.getInstance());
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                newList.add(item);
            }
        });
        return newList.stream().map(f -> fieldContext.mapToJavaSource(f)).collect(Collectors.joining(", ", "new HashSet<>(Arrays.asList(", "))"));
    }

    @NotNull
    static <T extends List<?>> String listToSource(FieldContext<T> fieldContext) {
        fieldContext.getImportList().add(Arrays.class);
        List<?> values = fieldContext.getInstanceToMap();
        List<Object> newList = new ArrayList<>();
        values.stream().forEach(item -> {
            boolean foundMatch = false;
            for (Field nodeField : fieldContext.getNodeFields()) {
                if (nodeField.getInstance().equals(item)) {
                    newList.add(nodeField.getInstance());
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                newList.add(item);
            }
        });
        return newList.stream().map(f -> fieldContext.mapToJavaSource(f)).collect(Collectors.joining(", ", "Arrays.asList(", ")"));
    }

    static String arrayListToSource(FieldContext<ArrayList<?>> fieldContext) {
        fieldContext.getImportList().add(ArrayList.class);
        String collect = listToSource(fieldContext);
        return "new ArrayList<>(" + collect + ")";
    }
}
