/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.groupby;

import com.fluxtion.dataflow.runtime.annotations.builder.FluxtionIgnore;
import com.fluxtion.dataflow.runtime.flowfunction.Stateful;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GroupByHashMap<K, V> implements GroupBy<K, V>, Stateful<GroupBy<K, V>> {
    @FluxtionIgnore
    private final Map<K, V> map = new HashMap<>();

    public GroupByHashMap<K, V> add(KeyValue<K, V> keyValue) {
        map.put(keyValue.getKey(), keyValue.getValue());
        return this;
    }

    public GroupByHashMap<K, V> fromMap(Map<K, V> fromMap) {
        reset();
        map.putAll(fromMap);
        return this;
    }

    @Override
    public GroupBy<K, V> reset() {
        map.clear();
        return this;
    }

    @Override
    public Map<K, V> toMap() {
        return map;
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public String toString() {
        return "GroupByHashMap{" +
                "map=" + map +
                '}';
    }
}
