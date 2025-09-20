/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.groupby;

import com.fluxtion.dataflow.runtime.annotations.NoTriggerReference;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableFunction;

import java.util.Map.Entry;

public class GroupByFilterFlowFunctionWrapper {

    @NoTriggerReference
    private final SerializableFunction mapFunction;
    private final transient GroupByHashMap outputCollection = new GroupByHashMap();

    public <T> GroupByFilterFlowFunctionWrapper(SerializableFunction<T, Boolean> mapFunction) {
        this.mapFunction = mapFunction;
    }

    //required for serialised version
    public <K, V> GroupBy<K, V> filterValues(Object inputMap) {
        return filterValues((GroupBy) inputMap);
    }

    public <K, V> GroupBy<K, V> filterValues(GroupBy<K, V> inputMap) {
        outputCollection.reset();
        inputMap.toMap().entrySet().forEach(e -> {
            Entry entry = (Entry) e;
            if ((boolean) mapFunction.apply(entry.getValue())) {
                outputCollection.toMap().put(entry.getKey(), entry.getValue());
            }
        });
        return outputCollection;
    }
}
