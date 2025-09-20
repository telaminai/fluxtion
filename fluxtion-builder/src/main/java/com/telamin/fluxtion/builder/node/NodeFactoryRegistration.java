/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
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
