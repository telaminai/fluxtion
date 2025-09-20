/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.node;

import com.telamin.fluxtion.runtime.audit.Auditor;

import java.util.Map;

/**
 * Holds all the currently registered nodes and factories in a graph. If a node
 * cannot be found then a {@link NodeFactory} will be used to create a new
 * instance, using the provided map for configuration.<p>
 * <p>
 * Users interact with the NodeRegistry through callbacks on the
 * {@link NodeFactory} interface.
 *
 * @author Greg Higgins
 * @see NodeFactory
 */
public interface NodeRegistry {

    /**
     * Find or create a node using a registered {@link NodeFactory}. The
     * generated node will have private scope and the name will be generated
     * from a {@link NodeNameProducer} strategy if the supplied name is null.
     *
     * @param <T>          The type of the node to be created.
     * @param clazz        The class of type T
     * @param config       a map used by a NodeFactory to create a node.
     * @param variableName the variable name in the SEP
     * @return The node that is referenced by the config map.
     */
    <T> T findOrCreateNode(Class<T> clazz, Map<String, Object> config, String variableName);

    /**
     * Find or create a node using a registered {@link NodeFactory}. The
     * generated node will have public scope and the name will be generated from
     * a {@link NodeNameProducer} strategy if the supplied name is null.
     *
     * @param <T>          The type of the node to be created.
     * @param clazz        The class of type T
     * @param config       a map used by a NodeFactory to create a node.
     * @param variableName the variable name in the SEP
     * @return The node that is referenced by the config map.
     */
    <T> T findOrCreatePublicNode(Class<T> clazz, Map<String, Object> config, String variableName);

    /**
     * Register a user created node with Fluxtion generator, no
     * {@link NodeFactory}'s will be used in this operation. The node will have
     * private scope and the name will be generated from a
     * {@link NodeNameProducer} strategy if the supplied name is null.
     *
     * @param <T>          The type of the node to be created.
     * @param node         The node to add to the SEP
     * @param variableName the variableName name of the node
     * @return
     */
    <T> T registerNode(T node, String variableName);

    /**
     * Register a user created node with Fluxtion generator, no
     * {@link NodeFactory}'s will be used in this operation. The node will have
     * public scope and the name will be generated from a
     * {@link NodeNameProducer} strategy if the supplied name is null.
     *
     * @param <T>          The type of the node to be created.
     * @param node         The node to add to the SEP
     * @param variableName the variableName name of the node
     * @return
     */
    <T> T registerPublicNode(T node, String variableName);

    /**
     * Register an {@link Auditor} with Fluxtion generator. The Auditor will
     * have public scope registered with the name provided.
     *
     * @param <T>         The Auditor class
     * @param node        The Auditor to register
     * @param auditorName the public name of the Auditor
     * @return The registered Auditor
     */
    <T extends Auditor> T registerAuditor(T node, String auditorName);
}
