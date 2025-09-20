/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.groupby;

import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableFunction;

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
