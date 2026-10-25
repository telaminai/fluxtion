/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.callback;

/**
 * Implemented by a dispatcher that discards the payloads of {@link QueuedPayloadOwner}s together with its own queued
 * triggers when a failed cycle's work is discarded. {@link CallbackDispatcherImpl} implements it. Deliberately not
 * part of {@link EventProcessorCallbackInternal}: an owner checks for it, so a third-party dispatcher need not.
 *
 * <p><b>Single-threaded</b>, like the processor and the owners' own payload queues: registration happens when an owner
 * queues a payload, on the processor's thread. Firing a callback from a foreign thread was never safe and races here
 * too. <b>An owner reused by another processor</b> moves its registration: it unregisters from the dispatcher it last
 * registered with before registering with the new one ({@link #reRegister}), so a failure in the old processor cannot
 * clear payloads queued for the new one. Registrations are otherwise kept for the dispatcher's lifetime; they are
 * bounded by the number of callback nodes and hold nothing per event.
 */
public interface QueuedPayloadRegistry {

    /** Register {@code owner}; registering the same owner again has no effect. */
    void registerPayloadOwner(QueuedPayloadOwner owner);

    /** Remove {@code owner}, if registered. */
    void unregisterPayloadOwner(QueuedPayloadOwner owner);

    /**
     * Move {@code owner}'s registration from {@code previous} (the value this method last returned for it, or null)
     * to {@code current}, the dispatcher it now queues through.
     *
     * @return the dispatcher the owner is now registered with, or null when {@code current} does not discard payloads
     */
    static Object reRegister(Object previous, Object current, QueuedPayloadOwner owner) {
        if (previous instanceof QueuedPayloadRegistry) {
            ((QueuedPayloadRegistry) previous).unregisterPayloadOwner(owner);
        }
        return registerIfSupported(current, owner);
    }

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
