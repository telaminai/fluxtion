/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.callback;

/**
 * A node that keeps the payloads of callbacks it has queued in its own store, separately from the trigger the
 * dispatcher queues (for example {@link CallBackNode} and {@link AbstractCallbackNode}, which hold a payload queue
 * and queue only an event that pops it). When a cycle fails, the dispatcher discards its queued triggers
 * ({@link EventProcessorCallbackInternal#discardQueuedCallbacks()}); every registered owner then discards its pending
 * payloads too, so a later, successful callback never consumes a payload of the failed cycle
 * (telaminai/fluxtion#36, telaminai/fluxtion-compiler#105).
 *
 * <p>An owner registers itself with a {@link QueuedPayloadRegistry} the first time it queues a payload.
 */
public interface QueuedPayloadOwner {

    /** Drop every payload queued for a callback that has not yet been dispatched. */
    void discardQueuedPayloads();
}
