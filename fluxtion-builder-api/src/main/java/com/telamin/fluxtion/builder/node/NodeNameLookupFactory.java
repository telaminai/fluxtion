/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.builder.node;

import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import com.telamin.fluxtion.runtime.audit.Auditor;
import com.telamin.fluxtion.runtime.audit.NodeNameAuditor;
import com.telamin.fluxtion.runtime.node.NodeNameLookup;

import java.util.Map;

public class NodeNameLookupFactory implements NodeFactory<NodeNameLookup> {

    private NodeNameAuditor nodeNameAuditor;

    @Override
    public NodeNameLookup createNode(Map<String, Object> config, NodeRegistry registry) {
        registry.registerAuditor(nodeNameAuditor, NodeNameLookup.DEFAULT_NODE_NAME);
        return nodeNameAuditor;
    }

    @Override
    public void preSepGeneration(GenerationContext context, Map<String, Auditor> auditorMap) {
        nodeNameAuditor = new NodeNameAuditor();
        auditorMap.put(NodeNameLookup.DEFAULT_NODE_NAME, nodeNameAuditor);
    }
}
