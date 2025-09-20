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
import com.telamin.fluxtion.runtime.audit.Auditor;
import com.telamin.fluxtion.runtime.audit.NodeNameAuditor;
import com.telamin.fluxtion.runtime.node.NodeNameLookup;

import java.util.Map;

public class NodeNameLookupFactory implements NodeFactory<NodeNameLookup> {

    private static NodeNameAuditor SINGLETON;

    @Override
    public NodeNameLookup createNode(Map<String, Object> config, NodeRegistry registry) {
        registry.registerAuditor(SINGLETON, NodeNameLookup.DEFAULT_NODE_NAME);
        return SINGLETON;
    }

    @Override
    public void preSepGeneration(GenerationContext context, Map<String, Auditor> auditorMap) {
        SINGLETON = new NodeNameAuditor();
        auditorMap.put(NodeNameLookup.DEFAULT_NODE_NAME, NodeNameLookupFactory.SINGLETON);
    }
}
