/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.time;

import com.telamin.fluxtion.runtime.annotations.feature.Experimental;
import com.telamin.fluxtion.runtime.annotations.runtime.ServiceRegistered;
import com.telamin.fluxtion.runtime.callback.AbstractCallbackNode;

/**
 * Represents a node that triggers scheduled actions using a {@link SchedulerService}.
 * This node extends {@link AbstractCallbackNode}, enabling it to fire callback events.
 * It supports scheduling events to occur after a specified delay. When the timer expires this node is the root of a
 * new event cycle.
 *
 * This class is marked as {@link Experimental}, meaning its features are subject to
 * change and its API should be used with caution in production environments.
 */
@Experimental
public class ScheduledTriggerNode extends AbstractCallbackNode<Object> implements ScheduledTrigger {

    private SchedulerService schedulerService;

    public ScheduledTriggerNode() {
        super();
    }

    public ScheduledTriggerNode(int filterId) {
        super(filterId);
    }

    @ServiceRegistered
    public void scheduler(SchedulerService scheduler) {
        this.schedulerService = scheduler;
    }

    @Override
    public void triggerAfterDelay(long millis) {
        if (schedulerService != null) {
            schedulerService.scheduleAfterDelay(millis, this::fireNewEventCycle);
        }
    }
}
