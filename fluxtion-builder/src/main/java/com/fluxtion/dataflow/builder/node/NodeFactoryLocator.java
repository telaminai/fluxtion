/*
 * SPDX-File Copyright: © 2019-2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */
package com.fluxtion.dataflow.builder.node;

import com.fluxtion.dataflow.builder.generation.context.GenerationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Loads a set of NodeFactory using the {@link ServiceLoader} support provided
 * by Java platform. New factories can be added to Fluxtion using the extension
 * mechanism described in {@link ServiceLoader} documentation.
 *
 * @author greg
 */
public class NodeFactoryLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeFactoryLocator.class);

    @SuppressWarnings("unchecked")
    public static Set<Class<? extends NodeFactory<?>>> nodeFactorySet() {
        LOGGER.debug("NodeFactory locator");
        ServiceLoader<NodeFactory<?>> loadServices;
        Set<Class<? extends NodeFactory<?>>> subTypes = new HashSet<>();
        Class<NodeFactory<?>> clazz = (Class<NodeFactory<?>>) (Object) NodeFactory.class;
        if (GenerationContext.SINGLETON != null && GenerationContext.SINGLETON.getClassLoader() != null) {
            LOGGER.debug("using custom class loader to search for factories");
            loadServices = ServiceLoader.load(clazz, GenerationContext.SINGLETON.getClassLoader());
        } else {
            loadServices = ServiceLoader.load(clazz);
        }
        loadServices.forEach((t) -> subTypes.add((Class<? extends NodeFactory<?>>) t.getClass()));
        LOGGER.debug("loaded NodeFactory services:{}", subTypes);
        return subTypes;
    }

}
