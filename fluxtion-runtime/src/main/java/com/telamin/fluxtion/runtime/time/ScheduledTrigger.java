/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.time;

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
