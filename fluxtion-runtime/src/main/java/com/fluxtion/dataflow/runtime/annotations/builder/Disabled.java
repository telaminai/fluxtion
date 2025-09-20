/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.annotations.builder;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Used in conjunction with generation annotations to allow the developer to
 * conditionally enable/disable processing of the annotation in context.
 *
 * @author 2024 gregory higgins.
 */
@Target(value = {TYPE, METHOD})
@Retention(value = CLASS)
@Documented
public @interface Disabled {

}
