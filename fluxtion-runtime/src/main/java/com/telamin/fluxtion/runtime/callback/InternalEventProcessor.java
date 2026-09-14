/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.callback;

import java.util.function.BooleanSupplier;

public interface InternalEventProcessor {

    void onEvent(Object event);

    void onEventInternal(Object event);

    default void triggerCalculation() {
    }

    /**
     * M50/W1 — told by {@link CallbackDispatcherImpl} whenever work is queued for this processor, and
     * again when the queue drains empty.
     *
     * <p>It exists so the generated processor can decide whether to drain by reading a field it owns,
     * instead of walking {@code processor → dispatcher → ArrayDeque → head/tail} on every event to be
     * told the queue is empty — which it is on every event of a graph that never re-enters.
     * <b>Measured on a three-event-type graph: that chase is 0.217 ns of a 5.60 ns event on a JIT, and
     * ~0.02 ns on native + PGO where the processor is scalar-replaced and there is nothing to chase.</b>
     *
     * <p>The dispatcher owns this call because <b>the dispatcher sees every queueing path</b> —
     * {@code fireCallback}, {@code fireIteratorCallback}, {@code processReentrantEvent(s)},
     * {@code queueReentrantEvent}. A flag the processor maintained itself would miss work queued by a
     * node holding the dispatcher directly, and the callback would be silently dropped.
     *
     * <p>Default is a no-op, so an existing processor is unaffected and keeps draining unconditionally.
     *
     * @param pending true when the queue may hold work, false when it is known empty
     */
    default void callbacksPending(boolean pending) {
    }

    void bufferEvent(Object event);

    boolean isDirty(Object node);

    BooleanSupplier dirtySupplier(Object node);

    void setDirty(Object node, boolean dirtyFlag);

    <T> T getNodeById(String id) throws NoSuchFieldException;

    default <T> T exportedService() {
        return (T) this;
    }

    default <T> T exportedService(Class<T> exportedServiceClass) {
        T svcExport = exportedService();
        return exportedServiceClass.isInstance(svcExport) ? exportedService() : null;
    }
}
