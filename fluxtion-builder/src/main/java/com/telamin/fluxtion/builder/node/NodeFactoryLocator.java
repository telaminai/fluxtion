/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.node;

import com.telamin.fluxtion.builder.generation.context.GenerationContext;
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
