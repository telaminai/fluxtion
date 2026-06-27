/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.annotations.builder.FluxtionIgnore;
import com.telamin.fluxtion.runtime.flowfunction.Stateful;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GroupByHashMap<K, V> implements GroupBy<K, V>, Stateful<GroupBy<K, V>> {
    @FluxtionIgnore
    private final Map<K, V> map = new HashMap<>();
    @FluxtionIgnore
    private transient GroupByDelta<K, V> delta = GroupByDelta.recomputeRequired();

    public GroupByHashMap<K, V> add(KeyValue<K, V> keyValue) {
        map.put(keyValue.getKey(), keyValue.getValue());
        return this;
    }

    /**
     * Record the change set this collection represents for the current cycle. Delta-aware operators
     * (P1+) set this; until then it stays {@link GroupByDelta#recomputeRequired()} so consumers
     * recompute (unchanged behaviour).
     */
    public GroupByHashMap<K, V> setDelta(GroupByDelta<K, V> delta) {
        this.delta = delta;
        return this;
    }

    @Override
    public GroupByDelta<K, V> delta() {
        return delta;
    }

    public GroupByHashMap<K, V> fromMap(Map<K, V> fromMap) {
        reset();
        map.putAll(fromMap);
        return this;
    }

    @Override
    public GroupBy<K, V> reset() {
        map.clear();
        delta = GroupByDelta.recomputeRequired();
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
