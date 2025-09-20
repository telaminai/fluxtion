/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */
package com.fluxtion.dataflow.builder.input;

import com.fluxtion.dataflow.builder.generation.context.GenerationContext;
import com.fluxtion.dataflow.builder.node.NodeFactory;
import com.fluxtion.dataflow.builder.node.NodeRegistry;
import com.fluxtion.dataflow.runtime.audit.Auditor;
import com.fluxtion.dataflow.runtime.input.SubscriptionManager;
import com.fluxtion.dataflow.runtime.input.SubscriptionManagerNode;

import java.util.Map;

/**
 * @author 2024 gregory higgins.
 */
public class SubscriptionManagerFactory implements NodeFactory<SubscriptionManager> {

    private static SubscriptionManagerNode SINGLETON;

    @Override
    public SubscriptionManager createNode(Map<String, ? super Object> config, NodeRegistry registry) {
        return registry.registerNode(SINGLETON, SubscriptionManagerNode.DEFAULT_NODE_NAME);
    }

    @Override
    public void preSepGeneration(GenerationContext context, Map<String, Auditor> auditorMap) {
        SINGLETON = new SubscriptionManagerNode();
        context.addOrUseExistingNode(SINGLETON);
    }
}
