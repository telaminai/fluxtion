/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.annotations.builder;

import com.fluxtion.dataflow.runtime.DataFlow;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a reference as an injection point for an instance. An injected instance
 * is created using a NodeFactory, see Fluxtion builder module.
 * <p>
 * <p>
 * If no NodeFactory is implemented for this type then a zero argument
 * constructor will be used to inject the reference. Properties of the injected
 * instance will be set using the configuration map, where keys are the member
 * properties of the class.<p>
 * <p>
 * Once an injected instance has been created it will be added to the SEP
 * execution graph as if a user had added the node manually. The injected node
 * will be inspected for annotations and processed by the Fluxtion Static Event
 * Compiler. New injected instances are recursively inspected until no more
 * injected instances are added to the execution graph. The recursive addition
 * of injected instances allows arbitrarily complex execution graphs be created
 * with a single {@literal @}Inject annotation.<p>
 * <p>
 * Injected references can be configured using {@link ConfigVariable} and
 * {@link Config} annotations.
 *
 * @author Greg Higgins
 * @see Config
 * @see ConfigVariable
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Inject {

    /**
     * Create and inject only a single instance in the generated SEP if true.
     *
     * @return singleton flag
     */
    boolean singleton() default false;

    /**
     * The name of the singleton if the singleton = true
     *
     * @return singleton name
     */
    String singletonName() default "";

    /**
     * The name of the NodeFactory.
     *
     * @return the NodeFactory name that will inject the instance into the graph
     */
    String factoryName() default "";

    /**
     * The name of the NodeFactory read from a variable in the annotated class.
     *
     * @return the NodeFactory name that will inject the instance into the graph
     */
    String factoryVariableName() default "";

    /**
     * Specify the qualifier of the instance to inject if using runtime instance injection with:
     * {@link DataFlow#injectNamedInstance(Object, String)}
     *
     * @return the runtime instance qualifer
     */
    String instanceName() default "";

    /**
     * Specify the qualifier of the instance to inject if using runtime instance injection with,
     * read from a variable in the annotated class.
     * <p>
     * {@link DataFlow#injectNamedInstance(Object, String)}
     *
     * @return the runtime instance qualifer
     */
    String instanceVariableName() default "";
}
