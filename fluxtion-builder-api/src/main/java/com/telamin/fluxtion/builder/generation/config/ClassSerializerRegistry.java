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

    /**
     * The registry for one target language, or {@link #NULL_REGISTRY} if none is registered.
     *
     * <p><b>This iterates every provider.</b> It used to take only the FIRST and return the null
     * registry when that one's language did not match — which worked for as long as exactly one
     * registry existed, and broke the moment a second did: whichever provider the service loader
     * happened to return first won, and asking for the other language silently got an EMPTY registry.
     * The symptom was Java losing its whole forty-type serialiser map the day a C++ registry was
     * added, so a {@code String} field stopped being supported and a constructor failed to match on it.
     *
     * <p>A lookup keyed by language that only ever examined one candidate was a latent fault in an
     * interface whose entire purpose is having several.
     */
    static ClassSerializerRegistry service(String targetLanguage) {
        ClassSerializerRegistry found = find(
                ServiceLoader.load(ClassSerializerRegistry.class,
                        ClassSerializerRegistry.class.getClassLoader()), targetLanguage);
        if (found != NULL_REGISTRY) {
            return found;
        }
        // Fall back to the context class loader, as before: a generator run from a build tool may not
        // see providers through this class's loader.
        return find(ServiceLoader.load(ClassSerializerRegistry.class), targetLanguage);
    }

    static ClassSerializerRegistry find(ServiceLoader<ClassSerializerRegistry> load,
                                        String targetLanguage) {
        for (ClassSerializerRegistry candidate : load) {
            if (candidate.targetLanguage() != null
                    && candidate.targetLanguage().equals(targetLanguage)) {
                return candidate;
            }
        }
        return NULL_REGISTRY;
    }
}
