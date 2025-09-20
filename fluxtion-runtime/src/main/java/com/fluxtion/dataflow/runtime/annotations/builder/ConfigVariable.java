/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.annotations.builder;

import java.lang.annotation.*;

/**
 * Marks a field as providing configuration for an injected instance. The
 * variable value is read at construction time creating a key/value pair.
 * Key/value pairs are added to a map which is supplied to a Nodefactory. A
 * NodeFactory uses the configuration map to build an injected instance.
 *
 * @author Greg Higgins
 * @see Inject
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(ConfigVariableList.class)
public @interface ConfigVariable {

    /**
     * The field to read for a configuration value.
     *
     * @return The field to read
     */
    String field() default "";

    /**
     * The key the value will be assigned to in the configuration map.
     *
     * @return the configuration key
     */
    String key();
}
