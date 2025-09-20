/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.node.NamedNode;
import com.telamin.fluxtion.runtime.node.NodeNameLookup;

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
