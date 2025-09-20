/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.annotations.NoTriggerReference;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;

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
