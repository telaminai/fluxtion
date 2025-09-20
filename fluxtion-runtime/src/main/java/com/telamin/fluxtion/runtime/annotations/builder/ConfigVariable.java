/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.runtime.annotations.builder;

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
