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

/**
 * provides query capability to determine if a node indicates it was dirty during this calculation cycle. A dirty node
 * propagates the event notification to dependents.
 */
public interface DirtyStateMonitor {
    boolean isDirty(Object node);

    BooleanSupplier dirtySupplier(Object node);

    void markDirty(Object node);

}
