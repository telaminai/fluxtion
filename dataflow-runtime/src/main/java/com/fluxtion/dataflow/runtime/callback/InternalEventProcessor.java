/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.callback;

import java.util.function.BooleanSupplier;

public interface InternalEventProcessor {

    void onEvent(Object event);

    void onEventInternal(Object event);

    default void triggerCalculation() {
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
