/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be invoked in the after event phase of event processing. A SEP
 * processes event handling methods in two phases:
 * <ul>
 * <li>Event in phase - processes handler methods in topological order
 * <li>After event phase - processes handler methods in reverse topological
 * order
 * </ul>
 * <p>
 * An OnEventComplete method will be called after all dependents have finished
 * processing any
 * after event phase methods. OnEventComplete methods are only called if the
 * following
 * conditions are met:
 * <ul>
 * <li>An event in phase handler is present in the same instance
 * <li>An event in phase handler is on the current execution path.
 * </ul>
 *
 * @author Greg Higgins
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AfterTrigger {

}
