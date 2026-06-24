/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.builder.callback;

import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import com.telamin.fluxtion.builder.generation.context.GenerationContextHolder;
import com.telamin.fluxtion.builder.node.NodeFactory;
import com.telamin.fluxtion.builder.node.NodeRegistry;
import com.telamin.fluxtion.runtime.audit.Auditor;
import com.telamin.fluxtion.runtime.callback.CallbackDispatcher;
import com.telamin.fluxtion.runtime.callback.CallbackDispatcherImpl;

import java.util.Map;

public class CallBackDispatcherFactory implements NodeFactory<CallbackDispatcher> {

    static CallbackDispatcherImpl dispatcher() {
        return dispatcher(GenerationContextHolder.current());
    }

    static CallbackDispatcherImpl dispatcher(GenerationContext context) {
        return context.<String, CallbackDispatcherImpl>getCache(CallBackDispatcherFactory.class)
                .computeIfAbsent(
                        CallbackDispatcher.DEFAULT_NODE_NAME,
                        key -> new CallbackDispatcherImpl());
    }

    @Override
    public CallbackDispatcher createNode(Map<String, Object> config, NodeRegistry registry) {
        return registry.registerNode(dispatcher(), CallbackDispatcher.DEFAULT_NODE_NAME);
    }

    @Override
    public void preSepGeneration(GenerationContext context, Map<String, Auditor> auditorMap) {
        context.addOrUseExistingNode(dispatcher(context));
    }
}
