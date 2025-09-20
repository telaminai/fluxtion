/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.audit;

import com.fluxtion.dataflow.runtime.node.NamedNode;
import com.fluxtion.dataflow.runtime.node.NodeNameLookup;

import java.util.HashMap;
import java.util.Map;

public class NodeNameAuditor implements Auditor, NodeNameLookup, NamedNode {

    private final transient Map<Object, String> node2NameMap = new HashMap<>();
    private final transient Map<String, Object> name2NodeMap = new HashMap<>();

    @Override
    public void nodeRegistered(Object node, String nodeName) {
        node2NameMap.put(node, nodeName);
        name2NodeMap.put(nodeName, node);
    }

    public String lookupInstanceName(Object node) {
        return node2NameMap.getOrDefault(node, "???");
    }

    @SuppressWarnings("unchecked")
    public <T> T getInstanceById(String id) throws NoSuchFieldException {
        T node = (T) name2NodeMap.get(id);
        if (node == null) {
            throw new NoSuchFieldException(id);
        }
        return node;
    }

    @Override
    public void init() {
        node2NameMap.clear();
    }

    @Override
    public String getName() {
        return NodeNameLookup.DEFAULT_NODE_NAME;
    }
}
