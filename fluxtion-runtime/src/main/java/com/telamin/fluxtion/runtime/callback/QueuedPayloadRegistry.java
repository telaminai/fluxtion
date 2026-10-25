/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.callback;

/**
 * Implemented by a dispatcher that discards the payloads of {@link QueuedPayloadOwner}s together with its own queued
 * triggers when a failed cycle's work is discarded. {@link CallbackDispatcherImpl} implements it. Deliberately not
 * part of {@link EventProcessorCallbackInternal}: an owner checks for it, so a third-party dispatcher need not.
 */
public interface QueuedPayloadRegistry {

    /** Register {@code owner}; registering the same owner again has no effect. */
    void registerPayloadOwner(QueuedPayloadOwner owner);

    /**
     * Register {@code owner} with {@code dispatcher} when the dispatcher discards payloads.
     *
     * @return the dispatcher the owner is now registered with, or null when it does not discard payloads
     */
    static Object registerIfSupported(Object dispatcher, QueuedPayloadOwner owner) {
        if (dispatcher instanceof QueuedPayloadRegistry) {
            ((QueuedPayloadRegistry) dispatcher).registerPayloadOwner(owner);
            return dispatcher;
        }
        return null;
    }
}
