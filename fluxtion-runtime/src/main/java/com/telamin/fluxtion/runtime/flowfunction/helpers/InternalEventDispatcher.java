/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.helpers;

import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import com.telamin.fluxtion.runtime.callback.EventDispatcher;

public class InternalEventDispatcher {

    @Inject
    private final EventDispatcher eventDispatcher;

    public InternalEventDispatcher(EventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }

    public InternalEventDispatcher() {
        this.eventDispatcher = null;
    }

    public void dispatchToGraph(Object event) {
        eventDispatcher.processReentrantEvent(event);
    }
}
