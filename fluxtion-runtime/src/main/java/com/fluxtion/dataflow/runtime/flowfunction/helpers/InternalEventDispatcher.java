/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.helpers;

import com.fluxtion.dataflow.runtime.annotations.builder.Inject;
import com.fluxtion.dataflow.runtime.callback.EventDispatcher;

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
