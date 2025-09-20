/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.util;

import java.util.*;

public interface CollectionHelper {
    @SafeVarargs
    static <E> List<E> listOf(E... elements) {
        if (elements.length == 0) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(Arrays.asList(elements));
        }
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    static <K, V> Map<K, V> ofEntries(Map.Entry<? extends K, ? extends V>... entries) {
        if (entries.length == 0) {
            return Collections.emptyMap();
        } else {
            HashMap<K, V> map = new HashMap<>(entries.length);
            for (Map.Entry<? extends K, ? extends V> entry : entries) {
                map.put(entry.getKey(), entry.getValue());
            }
            return map;
        }
    }
}
