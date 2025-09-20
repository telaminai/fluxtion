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

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE})
public @interface ExportService {

    /**
     * Determines whether the SEP will invoke dependents as part of the event
     * call chain. This has the effect of overriding the return value from the
     * event handler
     * method in the user class with the following effect:
     * <ul>
     * <li>true - use the boolean return value from event handler to determine
     * event propagation.
     * <li>false - permanently remove the event handler method from the
     * execution path
     * </ul>
     *
     * @return invoke dependents on update
     */
    boolean propagate() default true;
}
