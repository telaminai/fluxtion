/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.time;


import com.telamin.fluxtion.runtime.annotations.feature.Experimental;

/**
 * The SchedulerService interface provides mechanisms for scheduling actions to be executed
 * at a specific time or after a certain delay. It also includes methods for retrieving
 * the current time in various granularities (milliseconds, microseconds, and nanoseconds).
 *
 * This interface is marked as {@link Experimental}, indicating it is an early-stage feature
 * subject to changes, instability, or incomplete functionality. Use in production environments
 * with caution.
 */
@Experimental
public interface SchedulerService {

    /**
     * Schedules a task to execute at the specified wall-clock time.
     *
     * @param expireTime the absolute time, in milliseconds since the epoch, at which the task should be executed
     * @param expiryAction the action to perform when the specified time is reached
     * @return a unique identifier for the scheduled task
     */
    long scheduleAtTime(long expireTime, Runnable expiryAction);

    /**
     * Schedules a task to be executed after a specified delay.
     *
     * @param waitTime the delay time, in milliseconds, after which the task should be executed
     * @param expiryAction the action to be executed when the delay period has elapsed
     * @return a unique identifier for the scheduled task
     */
    long scheduleAfterDelay(long waitTime, Runnable expiryAction);

    long milliTime();

    long microTime();

    long nanoTime();
}
