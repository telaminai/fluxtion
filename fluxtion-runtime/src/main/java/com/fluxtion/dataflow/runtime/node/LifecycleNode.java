/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.node;

import com.fluxtion.dataflow.runtime.CloneableDataFlow;
import com.fluxtion.dataflow.runtime.annotations.Initialise;
import com.fluxtion.dataflow.runtime.annotations.Start;
import com.fluxtion.dataflow.runtime.annotations.Stop;
import com.fluxtion.dataflow.runtime.annotations.TearDown;

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
