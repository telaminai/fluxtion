/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */
package com.fluxtion.dataflow.builder.node;

import java.util.ServiceLoader;

/**
 * Implementing this interface allow users to extend the generation of the SEP
 * with customisable variable names for nodes. A default naming strategy will be
 * used if the registered NodeNameProducer returns null.<p>
 * <p>
 * An optional {@link #priority() } can be returned to Fluxtion generator,
 * allowing control of naming strategy resolution.
 *
 *
 * <h2>Registering factories</h2>
 * Fluxtion employs the {@link ServiceLoader} pattern to register user
 * implemented NodeNameProducer's. Please read the java documentation describing
 * the meta-data a node implementor must provide to register a node using
 * the {@link ServiceLoader} pattern.
 *
 * @author Greg Higgins
 */
public interface NodeNameProducer extends Comparable<NodeNameProducer> {

    /**
     * The mapping strategy for naming this node. The name returned will be used
     * in the generated source code as the variable name of the node.
     *
     * @param nodeToMap incoming node
     * @return the name of the node
     */
    String mappedNodeName(Object nodeToMap);

    /**
     * The priority of this naming strategy. Default value = 1000.
     *
     * @return naming strategy priority.
     */
    default int priority() {
        return 1000;
    }

    @Override
    default int compareTo(NodeNameProducer other) {
        return other.priority() - this.priority();
    }
}
