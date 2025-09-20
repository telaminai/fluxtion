/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.context;

import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import com.telamin.fluxtion.builder.node.NodeFactory;
import com.telamin.fluxtion.builder.node.NodeRegistry;
import com.telamin.fluxtion.runtime.audit.Auditor;
import com.telamin.fluxtion.runtime.context.DataFlowContext;
import com.telamin.fluxtion.runtime.node.MutableDataFlowContext;

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
