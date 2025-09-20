/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.flowfunction;

import com.fluxtion.dataflow.runtime.flowfunction.TriggeredFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.function.MapFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.groupby.GroupBy;

public class AbstractGroupByBuilder<K, V, T extends GroupBy<K, V>> extends FlowBuilder<T> {

    AbstractGroupByBuilder(TriggeredFlowFunction<T> eventStream) {
        super(eventStream);
    }

    <I, G extends GroupBy<K, V>>
    AbstractGroupByBuilder(MapFlowFunction<I, T, TriggeredFlowFunction<I>> eventStream) {
        super(eventStream);
    }
}