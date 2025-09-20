/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.time;

/**
 * Represents an entity capable of scheduling and triggering actions after a specified delay.
 *
 * This interface defines a contract for components that need to initiate an action
 * or event after a given period of time has elapsed. Implementations may utilize
 * various scheduling mechanisms to achieve this functionality.
 */
public interface ScheduledTrigger {
    /**
     * Triggers an action or event after the specified delay in milliseconds.
     *
     * @param millis the delay in milliseconds after which the action should be triggered
     */
    void triggerAfterDelay(long millis);
}
