/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.callback;

import com.fluxtion.dataflow.builder.generation.context.GenerationContext;
import com.fluxtion.dataflow.builder.node.NodeFactory;
import com.fluxtion.dataflow.builder.node.NodeRegistry;
import com.fluxtion.dataflow.runtime.audit.Auditor;
import com.fluxtion.dataflow.runtime.callback.CallbackDispatcher;
import com.fluxtion.dataflow.runtime.callback.CallbackDispatcherImpl;

import java.util.Map;

public class CallBackDispatcherFactory implements NodeFactory<CallbackDispatcher> {
    static CallbackDispatcherImpl SINGLETON;

    @Override
    public CallbackDispatcher createNode(Map<String, Object> config, NodeRegistry registry) {
        return registry.registerNode(SINGLETON, CallbackDispatcher.DEFAULT_NODE_NAME);
    }

    @Override
    public void preSepGeneration(GenerationContext context, Map<String, Auditor> auditorMap) {
        SINGLETON = new CallbackDispatcherImpl();
        context.addOrUseExistingNode(SINGLETON);
    }
}
