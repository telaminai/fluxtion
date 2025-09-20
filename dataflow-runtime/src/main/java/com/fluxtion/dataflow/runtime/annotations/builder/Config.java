/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.annotations.builder;

import java.lang.annotation.*;

/**
 * A static String based key value pair configuration item. The key/value pair
 * is added to a map which is supplied to a Nodefactory. A NodeFactory uses the
 * configuration map to build an injected instance.
 *
 * @author Greg Higgins
 * @see Inject
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(value = ConfigList.class)
public @interface Config {

    /**
     * The configuration value.
     *
     * @return The configuration value
     */
    String value();

    /**
     * The key the value will be assigned to in the configuration map.
     *
     * @return the configuration key
     */
    String key();

}
