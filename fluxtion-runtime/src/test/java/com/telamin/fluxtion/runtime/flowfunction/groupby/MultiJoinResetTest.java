/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.flowfunction.FlowSupplier;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MultiJoinResetTest {

    @Test
    public void parentResetClearsJoinedStateWithoutDereferencingNullLastKeyValue() {
        MutableGroupBy<String, Integer> leg = new MutableGroupBy<>();
        TestFlowSupplier<GroupBy<String, Integer>> supplier = new TestFlowSupplier<>(leg);
        MultiJoin<String, JoinedStats> join = new MultiJoin<>(String.class, JoinedStats::new);
        join.addJoin(supplier, JoinedStats::setValue);

        leg.put("FX", 108);
        join.groupByUpdated(join.getLegMappers().getFirst());
        assertEquals(108, join.toMap().get("FX").value);
        assertTrue(join.updated());

        leg.reset();
        join.groupByUpdated(join.getLegMappers().getFirst());
        assertTrue(join.toMap().isEmpty());
        assertFalse(join.updated());

        leg.put("FX", 20);
        join.groupByUpdated(join.getLegMappers().getFirst());
        assertEquals(20, join.toMap().get("FX").value);
        assertTrue(join.updated());
    }

    public static class JoinedStats {
        private int value;

        public void setValue(int value) {
            this.value = value;
        }
    }

    private static final class TestFlowSupplier<T> implements FlowSupplier<T> {
        private final T value;

        private TestFlowSupplier(T value) {
            this.value = value;
        }

        @Override
        public boolean hasChanged() {
            return true;
        }

        @Override
        public T get() {
            return value;
        }
    }

    private static final class MutableGroupBy<K, V> implements GroupBy<K, V> {
        private final Map<K, V> map = new HashMap<>();
        private KeyValue<K, V> lastKeyValue;

        private void put(K key, V value) {
            map.put(key, value);
            lastKeyValue = new KeyValue<>(key, value);
        }

        private void reset() {
            map.clear();
            lastKeyValue = null;
        }

        @Override
        public Map<K, V> toMap() {
            return map;
        }

        @Override
        public java.util.Collection<V> values() {
            return map.values();
        }

        @Override
        public KeyValue<K, V> lastKeyValue() {
            return lastKeyValue;
        }
    }
}
