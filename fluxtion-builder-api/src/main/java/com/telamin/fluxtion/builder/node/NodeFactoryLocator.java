/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.builder.node;

import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import com.telamin.fluxtion.builder.generation.context.GenerationContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads a set of NodeFactory using the {@link ServiceLoader} support provided
 * by Java platform. New factories can be added to Fluxtion using the extension
 * mechanism described in {@link ServiceLoader} documentation.
 *
 * @author greg
 */
public class NodeFactoryLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeFactoryLocator.class);

    /** Cache of factory sets per ClassLoader. */
    private static final ConcurrentHashMap<ClassLoader, Set<Class<? extends NodeFactory<?>>>> CACHE = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static Set<Class<? extends NodeFactory<?>>> nodeFactorySet() {
        LOGGER.debug("NodeFactory locator");
        ClassLoader cl = (GenerationContextHolder.currentOrNull() != null && GenerationContextHolder.currentOrNull().getClassLoader() != null)
                ? GenerationContextHolder.currentOrNull().getClassLoader()
                : null;

        ClassLoader cacheKey = cl != null ? cl : NodeFactoryLocator.class.getClassLoader();

        Set<Class<? extends NodeFactory<?>>> cached = CACHE.computeIfAbsent(cacheKey, k -> {
            ServiceLoader<NodeFactory<?>> loadServices;
            Set<Class<? extends NodeFactory<?>>> subTypes = new HashSet<>();
            Class<NodeFactory<?>> clazz = (Class<NodeFactory<?>>) (Object) NodeFactory.class;
            if (cl != null) {
                LOGGER.debug("using custom class loader to search for factories");
                loadServices = ServiceLoader.load(clazz, cl);
            } else {
                loadServices = ServiceLoader.load(clazz);
            }
            loadServices.forEach((t) -> subTypes.add((Class<? extends NodeFactory<?>>) t.getClass()));
            LOGGER.debug("loaded NodeFactory services:{}", subTypes);
            return Collections.unmodifiableSet(subTypes);
        });
        return new HashSet<>(cached);
    }

    /** Clear cache — useful for testing or when classpath changes. */
    public static void clearCache() {
        CACHE.clear();
    }

}
