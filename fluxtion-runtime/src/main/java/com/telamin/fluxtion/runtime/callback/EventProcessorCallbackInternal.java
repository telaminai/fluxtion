/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.callback;

public interface EventProcessorCallbackInternal extends CallbackDispatcher, DirtyStateMonitor {

    void dispatchQueuedCallbacks();

    /**
     * Discard everything queued by a cycle that failed, re-entrant events and callbacks alike, so none of the failed
     * cycle's work runs as part of a later one. Called by the processor when a node throws.
     */
    void discardQueuedCallbacks();

    void setEventProcessor(InternalEventProcessor eventProcessor);
}
