/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.audit;

/**
 * EventLogSource is registered with a {@link EventLogManager} at initialisation time. The
 * EventLogManager injects a configured {@link EventLogger} to this instance via
 * the {@link #setLogger(EventLogger)}  }. A
 * user implements this interface for a node in the execution graph to receive
 * a configured {@link EventLogger}.<br>
 * <p>
 * The node writes to the EventLogger in any of the lifecycle or event methods
 * to record data and the {@code EventLogManager} handles the formatting and
 * marshalling of log records.
 *
 * @author Greg Higgins (greg.higgins@v12technology.com)
 */
public interface EventLogSource {

    /**
     * A configured {@link EventLogger} this EventLogSource can write events to.
     *
     * @param log log target
     */
    void setLogger(EventLogger log);

}
