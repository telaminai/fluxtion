/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.builder.generation.config;


import com.telamin.fluxtion.builder.generation.serialiser.FieldContext;

import java.util.Collections;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;

public interface ClassSerializerRegistry {

    String targetLanguage();

    default Map<Class<?>, Function<FieldContext, String>> classSerializerMap() {
        return Collections.emptyMap();
    }

    ClassSerializerRegistry NULL_REGISTRY = new ClassSerializerRegistry() {
        @Override
        public String targetLanguage() {
            return "";
        }
    };

    static ClassSerializerRegistry service(String targetLanguage) {
        ServiceLoader<ClassSerializerRegistry> load = ServiceLoader.load(ClassSerializerRegistry.class, ClassSerializerRegistry.class.getClassLoader());
        ClassSerializerRegistry service = NULL_REGISTRY;
        if (load.iterator().hasNext()) {
            service = load.iterator().next();
            if (service.targetLanguage().equals(targetLanguage)) {
                return service;
            }
        } else {
            load = ServiceLoader.load(ClassSerializerRegistry.class);
            if (load.iterator().hasNext()) {
                service = load.iterator().next();
                if (service.targetLanguage().equals(targetLanguage)) {
                    return service;
                }
            }
        }
        return NULL_REGISTRY;
    }
}
