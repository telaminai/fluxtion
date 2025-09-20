/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */
package com.fluxtion.dataflow.builder.node;

import com.fluxtion.dataflow.builder.generation.context.GenerationContext;
import com.fluxtion.dataflow.runtime.annotations.builder.Config;
import com.fluxtion.dataflow.runtime.annotations.builder.Inject;
import com.fluxtion.dataflow.runtime.audit.Auditor;

import java.util.Map;
import java.util.ServiceLoader;

/**
 * A node for creating instances. The SEP will call this method to create
 * nodes.<p>
 *
 * <h2>Instance re-use</h2>
 * To ensure that node instances are re-used there are two approaches:
 * <ul>
 * <li>The node caches the node, and returns the same instance for the same
 * configuration.
 * <li>The node creates new nodes and the node overrides hashcode and equals.
 * The {@link NodeRegistry} will consult a map for an equivalent node. If an
 * equivalent node is found then the newly created node will be discarded and
 * the existing instance used.
 * </ul>
 * <p>
 * <h2>Registering factories</h2>
 * Fluxtion employs the {@link ServiceLoader} pattern to register user
 * implemented NodeFactories. Please read the java documentation describing the
 * meta-data a node implementor must provide to register a node using the
 * {@link ServiceLoader} pattern.
 *
 * @param <T>
 * @author Greg Higgins
 */
public interface NodeFactory<T> {

    /**
     * The key in the map passed to the NodeFactory instance that holds the {@link java.lang.reflect.Field} type
     * information for the target field.
     */
    String FIELD_KEY = "NodeFactory.InjectField.TypeInfo";
    /**
     * The key in the map passed to the NodeFactory instance that holds the name of the instance to be injected when
     * using information for the target field.
     */
    String INSTANCE_KEY = "NodeFactory.InjectField.InstanceName";

    /**
     * NodeFactory writer must implement this method to generate instances of
     * nodes. The Fluxtion compiler will call this method when an {@link Inject}
     * instance is created. {@link Config} variables are used to populate the
     * config map.
     *
     * @param config   map configuration
     * @param registry The node registry of the current generation contextß
     * @return The newly created node instance
     */
    T createNode(Map<String, Object> config, NodeRegistry registry);

    /**
     * Callback invoked by Fluxtion generator after the generated SEP has been
     * registered in the{@link GenerationContext}
     *
     * @param config   map configuration
     * @param registry The node registry of the current generation context
     * @param instance the newly created instance
     */
    default void postInstanceRegistration(Map<String, Object> config, NodeRegistry registry, T instance) {
    }

    /**
     * If the node generates a class for this SEP, this callback will indicate
     * the desired target.
     *
     * @param targetLanguage target language for generation
     */
    default void setTargetLanguage(String targetLanguage) {
    }

    /**
     * If the node generates a class for this SEP, this callback gives the node
     * access to the GenerationContext before generation.
     *
     * @param context    The context the Fluxtion SEC compiler uses
     * @param auditorMap auditors map for client to populate, these will be added to the generated CloneableDataFlow
     */
    default void preSepGeneration(GenerationContext context, Map<String, Auditor> auditorMap) {
    }

    default String factoryName() {
        return "";
    }

    /**
     * Override the injection type inferred by the type variable
     *
     * @return
     */
    default Class<T> injectionType() {
        return null;
    }

}
