/*
 * SPDX-File Copyright: © 2024-2025.  Gregory Higgins <greg.higgins@v12technology.com>
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
 * <li>After event phase - processes handler methods in reverse topological order
 * </ul>
 * <p>
 * An AfterEvent method will be called after all dependents have finished processing any
 * after event phase methods. AfterEvent handlers are always called regardless of the incoming
 * event.
 *
 * @author Greg Higgins
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AfterEvent {

}
