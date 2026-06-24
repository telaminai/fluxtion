/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.builder.input;

import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import com.telamin.fluxtion.builder.node.NodeFactory;
import com.telamin.fluxtion.builder.node.NodeRegistry;
import com.telamin.fluxtion.runtime.audit.Auditor;
import com.telamin.fluxtion.runtime.input.SubscriptionManager;
import com.telamin.fluxtion.runtime.input.SubscriptionManagerNode;

import java.util.Map;

/**
 * @author 2024 gregory higgins.
 */
public class SubscriptionManagerFactory implements NodeFactory<SubscriptionManager> {

    private SubscriptionManagerNode subscriptionManager;

    @Override
    public SubscriptionManager createNode(Map<String, ? super Object> config, NodeRegistry registry) {
        return registry.registerNode(subscriptionManager, SubscriptionManagerNode.DEFAULT_NODE_NAME);
    }

    @Override
    public void preSepGeneration(GenerationContext context, Map<String, Auditor> auditorMap) {
        subscriptionManager = new SubscriptionManagerNode();
        context.addOrUseExistingNode(subscriptionManager);
    }
}
