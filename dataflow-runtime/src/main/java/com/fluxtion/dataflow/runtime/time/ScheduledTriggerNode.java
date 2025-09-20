/*
 * SPDX-FileCopyrightText: © 2025 Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.time;

import com.fluxtion.dataflow.runtime.annotations.feature.Experimental;
import com.fluxtion.dataflow.runtime.annotations.runtime.ServiceRegistered;
import com.fluxtion.dataflow.runtime.callback.AbstractCallbackNode;

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
