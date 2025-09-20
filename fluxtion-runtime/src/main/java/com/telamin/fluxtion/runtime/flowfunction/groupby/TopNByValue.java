/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class TopNByValue {
    private final int count;
    public SerializableFunction comparing;

    public TopNByValue(int count) {
        this.count = count;
    }

    //required for serialised version
    public <K, V> List<Entry<K, V>> filter(Object obj) {
        return filter((GroupBy) obj);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <K, V> List<Entry<K, V>> filter(GroupBy groupBy) {
        return (List<Entry<K, V>>) new ArrayList<>(groupBy.toMap().entrySet()).stream()
                .sorted((Comparator<Entry>) (c1, c2) -> {
                    if (comparing != null) {
                        return ((Comparable) comparing.apply(c2.getValue())).compareTo(
                                comparing.apply(c1.getValue())
                        );
                    }
                    return ((Comparable) c2.getValue()).compareTo(c1.getValue());
                })
                .limit(count)
                .collect(Collectors.toList());
    }
}
