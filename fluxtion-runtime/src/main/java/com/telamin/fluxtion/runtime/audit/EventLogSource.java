/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.runtime.audit;

/**
 * Marker interface for nodes that want to participate in structured audit logging.
 * <p>
 * How to get an EventLogger at runtime:
 * <ul>
 *   <li>Implement this interface on your node, or</li>
 *   <li>Extend {@link EventLogNode}, which already implements this interface and exposes a protected
 *   {@code auditLog} field for convenience.</li>
 * </ul>
 * During graph initialisation the {@link EventLogManager} discovers all EventLogSource instances and
 * injects a configured {@link EventLogger} by calling {@link #setLogger(EventLogger)}. Before injection
 * the logger defaults to a {@link NullEventLogger} and safely discards writes.
 * <p>
 * Once injected, the node can write key/value pairs in its lifecycle or event handler methods, while
 * the EventLogManager aggregates and emits per-node records in topological execution order.
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
