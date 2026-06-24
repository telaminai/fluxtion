/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.builder.callback;

import com.telamin.fluxtion.builder.node.NodeFactory;
import com.telamin.fluxtion.builder.node.NodeRegistry;
import com.telamin.fluxtion.runtime.callback.CallbackDispatcher;
import com.telamin.fluxtion.runtime.callback.EventDispatcher;

import java.util.Map;

public class EventDispatcherFactory implements NodeFactory<EventDispatcher> {

    @Override
    public EventDispatcher createNode(Map<String, Object> config, NodeRegistry registry) {
        return registry.registerNode(CallBackDispatcherFactory.dispatcher(), CallbackDispatcher.DEFAULT_NODE_NAME);
    }
}
