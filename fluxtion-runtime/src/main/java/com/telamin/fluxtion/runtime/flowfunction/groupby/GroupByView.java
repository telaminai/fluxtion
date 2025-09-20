/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import java.util.Collection;
import java.util.Map;

public class GroupByView<K, V> implements GroupBy<K, V> {

    private KeyValue<K, V> keyValue;
    private GroupBy<K, V> groupBy;

    public GroupByView() {
    }

    public GroupByView(KeyValue<K, V> keyValue, GroupBy<K, V> groupBy) {
        this.keyValue = keyValue;
        this.groupBy = groupBy;
    }

    public KeyValue<K, V> getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(KeyValue<K, V> keyValue) {
        this.keyValue = keyValue;
    }

    public GroupBy<K, V> getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(GroupBy<K, V> groupBy) {
        this.groupBy = groupBy;
    }

    @Override
    public V lastValue() {
        return keyValue.getValue();
    }

    @Override
    public KeyValue<K, V> lastKeyValue() {
        return keyValue;
    }

    @Override
    public Map<K, V> toMap() {
        return groupBy.toMap();
    }

    @Override
    public Collection<V> values() {
        return groupBy.values();
    }

    public void reset() {
        groupBy = GroupBy.emptyCollection();
        keyValue = GroupBy.emptyKey();
    }
}

