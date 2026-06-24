/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
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
