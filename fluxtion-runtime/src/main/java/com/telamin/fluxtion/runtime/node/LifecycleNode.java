/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.node;

import com.telamin.fluxtion.runtime.CloneableDataFlow;
import com.telamin.fluxtion.runtime.annotations.Initialise;
import com.telamin.fluxtion.runtime.annotations.Start;
import com.telamin.fluxtion.runtime.annotations.Stop;
import com.telamin.fluxtion.runtime.annotations.TearDown;

public interface LifecycleNode {

    /**
     * callback received before any events are processed by the Static event
     * processor. Init methods are invoked in topological order. The {@link CloneableDataFlow}
     * can only process events once init has completed.
     */
    @Initialise
    default void init() {
    }

    /**
     * invoke after init. Start methods are invoked in topological order. Start/stop can attach application nodes to
     * a life cycle method when the {@link CloneableDataFlow} can process methods
     */
    @Start
    default void start() {
    }

    /**
     * invoke after start. Stop methods are invoked in reverse-topological order. Start/stop can attach application nodes to
     * a life cycle method when the {@link CloneableDataFlow} can process methods
     */
    @Stop
    default void stop() {
    }

    /**
     * callback received after all events are processed by the Static event
     * processor, and no more are expected. tearDown methods are invoked in
     * reverse-topological order.
     */
    @TearDown
    default void tearDown() {
    }
}
