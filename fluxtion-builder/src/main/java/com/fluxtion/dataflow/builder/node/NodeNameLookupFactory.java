/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.node;

import com.fluxtion.dataflow.builder.generation.context.GenerationContext;
import com.fluxtion.dataflow.runtime.audit.Auditor;
import com.fluxtion.dataflow.runtime.audit.NodeNameAuditor;
import com.fluxtion.dataflow.runtime.node.NodeNameLookup;

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
