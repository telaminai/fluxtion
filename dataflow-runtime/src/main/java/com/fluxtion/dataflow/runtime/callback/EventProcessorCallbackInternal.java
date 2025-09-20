/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.callback;

public interface EventProcessorCallbackInternal extends CallbackDispatcher, DirtyStateMonitor {

    void dispatchQueuedCallbacks();

    void setEventProcessor(InternalEventProcessor eventProcessor);
}
