/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.runtime.lifecycle;

import com.telamin.fluxtion.runtime.CloneableDataFlow;

/**
 * Lifecycle events that are issued by a Static event processor. Any node in the
 * execution graph can implement this interface and will receive the relevant
 * callbacks.
 * <p>
 * These notifications are generally used to initialise and teardown a node in
 * the graph before any events have been received. The static event processor
 * guarantees:
 * <ul>
 * <li>the init method will be called before any events are processed by the SEP
 * <li>the teardown method will be called after the sep is closed down
 * <li>Init methods are invoked in topological order
 * <li>teardown methods are invoked in reverse-topological order
 * <li>Start/stop methods are available for application life cycle call backs</li>
 * <li>Start/stop do not need to be called for event processing to function</li>
 * </ul>
 *
 * @author Greg Higgins
 */
public interface Lifecycle {

    /**
     * callback received before any events are processed by the Static event
     * processor. Init methods are invoked in topological order. The {@link CloneableDataFlow}
     * can only process events once init has completed.
     */
    default void init(){}

    /**
     * callback received after all events are processed by the Static event
     * processor, and no more are expected. tearDown methods are invoked in
     * reverse-topological order.
     */
    default void tearDown(){}

    /**
     * Callback received after init, start methods are invoked in topological order. Application nodes can attach
     * a life cycle method to {@link CloneableDataFlow#start()}. There are no guarantees that start will be called, it
     * is the decision of the application to call this method if at all. It is an application error to call start before
     * init.
     */
    default void start() {
    }

    /**
     * Callback received after all start methods have completed, startComplete methods are invoked in topological order.
     * Application nodes can attach a life cycle method to {@link CloneableDataFlow#startComplete()}. There are no guarantees that
     * start will be called, it is the decision of the application to call this method if at all. It is an application
     * error to call startComplete before init.
     */
    default void startComplete() {
    }

    /**
     * invoke after start. Stop methods are invoked in reverse-topological order. Start/stop can attach application nodes to
     * a life cycle method when the {@link CloneableDataFlow} can process methods
     */
    default void stop() {
    }

    enum LifecycleEvent {Init, TearDown, Start, StartComplete, Stop, BatchPause, BatchEnd}
}
