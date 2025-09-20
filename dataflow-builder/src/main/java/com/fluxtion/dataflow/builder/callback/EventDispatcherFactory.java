/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.callback;

import com.fluxtion.dataflow.builder.node.NodeFactory;
import com.fluxtion.dataflow.builder.node.NodeRegistry;
import com.fluxtion.dataflow.runtime.callback.CallbackDispatcher;
import com.fluxtion.dataflow.runtime.callback.EventDispatcher;

import java.util.Map;

public class EventDispatcherFactory implements NodeFactory<EventDispatcher> {

    @Override
    public EventDispatcher createNode(Map<String, Object> config, NodeRegistry registry) {
        return registry.registerNode(CallBackDispatcherFactory.SINGLETON, CallbackDispatcher.DEFAULT_NODE_NAME);
    }
}
