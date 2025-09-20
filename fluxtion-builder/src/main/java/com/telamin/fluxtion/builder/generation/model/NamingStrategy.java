/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.generation.model;

import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import com.telamin.fluxtion.builder.node.NodeNameProducer;
import com.telamin.fluxtion.runtime.node.NamedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ServiceLoader;

/**
 * @author gregp
 */
class NamingStrategy implements NodeNameProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NamingStrategy.class);
    private ArrayList<NodeNameProducer> namingStrategies;
    private int count;

    public NamingStrategy() {
        loadServices();
    }

    public final void loadServices() {
        LOGGER.debug("NamingStrategy (re)loading naming strategies");
        ServiceLoader<NodeNameProducer> loadServices;
        namingStrategies = new ArrayList<>();
        if (GenerationContext.SINGLETON != null && GenerationContext.SINGLETON.getClassLoader() != null) {
            LOGGER.debug("using custom class loader to search for NodeNameProducer");
            loadServices = ServiceLoader.load(NodeNameProducer.class, GenerationContext.SINGLETON.getClassLoader());
        } else {
            loadServices = ServiceLoader.load(NodeNameProducer.class);
        }
        loadServices.forEach(namingStrategies::add);
        Collections.sort(namingStrategies);
        LOGGER.debug("sorted naming strategies : {}", namingStrategies);
    }

    @Override
    public String mappedNodeName(Object nodeToMap) {
        String mappedName = null;
        for (NodeNameProducer namingStrategy : namingStrategies) {
            mappedName = namingStrategy.mappedNodeName(nodeToMap);
            if (mappedName != null)
                break;
        }
        if (mappedName == null && nodeToMap instanceof NamedNode) {
            mappedName = ((NamedNode) nodeToMap).getName();
        }
        if (mappedName == null) {
            mappedName = nodeToMap.getClass().getSimpleName() + "_" + count++;
            mappedName = Character.toLowerCase(mappedName.charAt(0)) + (mappedName.length() > 1 ? mappedName.substring(1) : "");
        }
        return mappedName;
    }

}
