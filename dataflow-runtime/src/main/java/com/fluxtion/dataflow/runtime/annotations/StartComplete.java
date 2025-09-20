/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.annotations;

import com.fluxtion.dataflow.runtime.lifecycle.Lifecycle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as bound to {@link Lifecycle#startComplete()} phase. A valid
 * startComplete method accepts no arguments.
 *
 * @author 2024 gregory higgins.
 * @see Lifecycle
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface StartComplete {

}
