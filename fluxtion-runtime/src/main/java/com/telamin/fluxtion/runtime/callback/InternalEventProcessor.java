/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.callback;

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
