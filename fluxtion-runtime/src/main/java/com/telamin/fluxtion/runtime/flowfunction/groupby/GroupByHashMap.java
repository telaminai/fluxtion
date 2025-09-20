/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
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
