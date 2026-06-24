/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction;

public interface TriggeredFlowFunction<T> extends FlowFunction<T>, FlowSupplier<T> {
    void setUpdateTriggerNode(Object updateTriggerNode);

    void setPublishTriggerNode(Object publishTriggerNode);

    void setResetTriggerNode(Object resetTriggerNode);

    void setPublishTriggerOverrideNode(Object publishTriggerOverrideNode);
}
