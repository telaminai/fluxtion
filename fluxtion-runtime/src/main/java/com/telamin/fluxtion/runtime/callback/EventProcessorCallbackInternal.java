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
     *
     * <p><b>New and abstract in 1.1.3</b>, deliberately: an implementation that did not discard would silently let a
     * failed cycle's work run later. An implementation drops every queued trigger and event, resets any dispatching
     * state a throw inside the drain left set, calls {@code callbacksPending(false)}, and, if it is a
     * {@link QueuedPayloadRegistry}, tells its {@link QueuedPayloadOwner}s to discard their payloads. Generated
     * processors call it only for a client runtime of 1.1.3 or later. See {@code docs/runtime/failed-event-cycles.md}.
     */
    void discardQueuedCallbacks();

    void setEventProcessor(InternalEventProcessor eventProcessor);
}
