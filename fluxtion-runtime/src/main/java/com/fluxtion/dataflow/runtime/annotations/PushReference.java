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
 * Marks a member that is part of the node set in the SEP as a push reference.
 * Normally the event wave starts at the target of the reference and then
 * notifies the source of the reference. A push reference inverts the event wave
 * so the source of the reference is notified before the target. This allows the
 * source to push data onto the target. Event notification of the target will occur
 * after the push.
 *
 * @author Greg Higgins
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PushReference {

}
