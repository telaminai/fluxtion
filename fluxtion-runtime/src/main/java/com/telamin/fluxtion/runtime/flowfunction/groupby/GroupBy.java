/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
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

    /**
     * The keys changed in the current event cycle (a changelog) — lets downstream operators and FSQL
     * typed output update in O(Δ) rather than re-scanning the whole group. See {@link GroupByDelta}.
     *
     * <p><b>Defaults to {@link GroupByDelta#recomputeRequired()}</b> — a producer that does not emit
     * deltas forces consumers to recompute/diff from {@link #toMap()}. A delta is an optimisation, not
     * a correctness boundary, so this is defaulted (not abstract) by design.
     */
    default GroupByDelta<K, V> delta() {
        return GroupByDelta.recomputeRequired();
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
