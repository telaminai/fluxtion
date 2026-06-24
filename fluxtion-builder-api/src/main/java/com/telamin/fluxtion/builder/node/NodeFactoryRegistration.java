/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.builder.node;

import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import lombok.EqualsAndHashCode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Registered {@link NodeFactory} available to support {@link Inject}
 * annotation
 */
@EqualsAndHashCode
public final class NodeFactoryRegistration {

    /**
     * The set of node classes used for node creation, each node must
     * have default constructor so the SEP can instantiate the node.
     * The classes in factoryClassSet are instantiated and merged into the
     * factorySet instances.
     */
    public final Set<Class<? extends NodeFactory<?>>> factoryClassSet;

    /**
     * The node instances registered that can create new instances of
     * nodes.
     */
    public final Set<NodeFactory<?>> factorySet;

    public NodeFactoryRegistration(Set<Class<? extends NodeFactory<?>>> factoryList) {
        this(factoryList, null);
    }

    public NodeFactoryRegistration(NodeFactory<?>... factorySet) {
        this(null, new HashSet<>(Arrays.asList(factorySet)));
    }

    public NodeFactoryRegistration(Set<Class<? extends NodeFactory<?>>> factoryList, Set<NodeFactory<?>> factorySet) {
        this.factoryClassSet = factoryList == null ? new HashSet<>() : factoryList;
        this.factorySet = factorySet == null ? new HashSet<>() : factorySet;
    }
}
