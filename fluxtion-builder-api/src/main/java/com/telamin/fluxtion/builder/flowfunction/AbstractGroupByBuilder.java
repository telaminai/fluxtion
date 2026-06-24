/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.builder.flowfunction;

import com.telamin.fluxtion.runtime.flowfunction.TriggeredFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.MapFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy;

public class AbstractGroupByBuilder<K, V, T extends GroupBy<K, V>> extends FlowBuilder<T> {

    AbstractGroupByBuilder(TriggeredFlowFunction<T> eventStream) {
        super(eventStream);
    }

    <I, G extends GroupBy<K, V>>
    AbstractGroupByBuilder(MapFlowFunction<I, T, TriggeredFlowFunction<I>> eventStream) {
        super(eventStream);
    }
}