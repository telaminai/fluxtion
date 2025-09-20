/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be called in a node when a dependency has processed an
 * event. Identifying which parent has changed can be useful when a node has
 * multiple dependencies on differing execution paths.
 * OnParentUpdate gives more granular notification than OnEvent, by identifying
 * which parents have updated. The marked method has a single argument, the type
 * of the parent.<p>
 * <p>
 * The marked method(s) will be invoked during the event in phase before any
 * {@link OnTrigger} methods in this node are invoked.
 * <p>
 * A SEP processes event handling methods in two phases:
 * <ul>
 * <li>Event in phase - processes handler methods in topological order
 * <li>After event phase - processes handler methods in reverse topological
 * order
 * </ul>
 *
 * <h2>Parent resolution</h2>
 * The type of the argument in the OnParentUpdate method is used to
 * resolve the parent to monitor. Optionally a {@link #value()} specifies the
 * field name of the parent to monitor. If multiple parents exist within this
 * class of the same type, type resolution is non-deterministic, specifying the
 * value predictably determines which parent is monitored.
 *
 * <h2>collections</h2>
 * If the OnParentUpdate method points to a collection, the instance inside the
 * collection that is updated will be passed to the method as an argument. As
 * multiple instances inside a collection maybe updated in an execution cycle,
 * the same OnParentUpdate method may be called multiple times in that cycle.
 *
 * @author Greg Higgins
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnParentUpdate {

    /**
     * The variable name of the parent reference to bind listen notifications
     * to.
     *
     * @return The variable name of the parent to monitor
     */
    String value() default "";

    /**
     * determines whether guards are present on the marked method.Setting
     * the value to false will ensure the callback is always called regardless
     * of the dirty state of the parent node.
     *
     * @return guarding of OnParentUpdate callback
     */
    boolean guarded() default true;
}
