/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import lombok.Value;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public interface GroupBy<K, V> {

    KeyValue<?, ?> KV_KEY_VALUE = new KeyValue<>(null, null);

    Map<K, V> toMap();

    Collection<V> values();

    default V lastValue() {
        return null;
    }

    default KeyValue<K, V> lastKeyValue() {
        return emptyKey();
    }

    @SuppressWarnings("unchecked")
    static <K, V> KeyValue<K, V> emptyKey() {
        return (KeyValue<K, V>) KV_KEY_VALUE;
    }

    @Value
    class KeyValue<K, V> {
        K key;
        V value;

        public Double getValueAsDouble() {
            return (Double) value;
        }

        public Long getValueAsLong() {
            return (Long) value;
        }

        public Integer getValueAsInt() {
            return (Integer) value;
        }
    }

    static <K, V> GroupBy<K, V> emptyCollection() {
        return new EmptyGroupBy<>();
    }

    class EmptyGroupBy<K, V> implements GroupBy<K, V> {
        @Override
        public V lastValue() {
            return null;
        }

        @Override
        public KeyValue<K, V> lastKeyValue() {
            return null;
        }

        @Override
        public Map<K, V> toMap() {
            return Collections.emptyMap();
        }

        @Override
        public Collection<V> values() {
            return Collections.emptyList();
        }
    }
}
