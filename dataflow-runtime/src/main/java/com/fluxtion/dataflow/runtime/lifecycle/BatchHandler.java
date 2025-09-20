/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.lifecycle;

/**
 * A callback interface used to signify that a transaction of events have been
 * received and complete. A common usage pattern is to process a set of events
 * before publishing/exposing state changes outside of the Static Event
 * Processor. An example would be a set of bank transfers broken into separate
 * events:
 *
 * <ul>
 * <li>DeleteEvent - delete amount from account A
 * <li>AddEvent - add to account B from A
 * <li>DeleteEvent - delete amount from account C
 * <li>AddEvent - add to account B from C
 * <li>AddEvent - add to account A from D
 * <li>DeleteEvent - delete amount from account D
 * <li>batchEnd() - publish updated accounts for A,B,C,D
 * </ul>
 * <p>
 * The batchPause callback is used to tell the static event processor more
 * messages are expected but have not been received yet.
 *
 * @author Greg Higgins
 */
public interface BatchHandler {

    /**
     * Indicates more events are expected, but there is an unknown pause in the message flow
     */
    void batchPause();

    /**
     * Indicates all events for a transaction have been received.
     */
    void batchEnd();
}
