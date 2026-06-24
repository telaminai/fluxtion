/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
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
