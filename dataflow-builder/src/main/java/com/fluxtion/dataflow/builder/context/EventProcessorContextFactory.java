/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */
package com.fluxtion.dataflow.builder.context;

import com.fluxtion.dataflow.builder.generation.context.GenerationContext;
import com.fluxtion.dataflow.runtime.context.DataFlowContext;
import com.fluxtion.dataflow.runtime.node.MutableDataFlowContext;
import com.fluxtion.dataflow.builder.node.NodeFactory;
import com.fluxtion.dataflow.builder.node.NodeRegistry;
import com.fluxtion.dataflow.runtime.audit.Auditor;

import java.util.Map;

/**
 * @author 2024 gregory higgins.
 */
public class EventProcessorContextFactory implements NodeFactory<DataFlowContext> {

    private static MutableDataFlowContext SINGLETON;

    @Override
    public DataFlowContext createNode(Map<String, ? super Object> config, NodeRegistry registry) {
        return registry.registerNode(SINGLETON, DataFlowContext.DEFAULT_NODE_NAME);
    }

    @Override
    public void preSepGeneration(GenerationContext context, Map<String, Auditor> auditorMap) {
        SINGLETON = new MutableDataFlowContext();
        context.addOrUseExistingNode(SINGLETON);
    }
}
