/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.annotations.builder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A holder for an array of {@link ConfigVariable} variables. The collection of {@link Config}
 * items populates a map. The configuration map is used by a Nodefactory to build an
 * injected instance.
 *
 * @author Greg Higgins
 * @see ConfigVariable
 * @see Inject
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigVariableList {
    ConfigVariable[] value();
}
