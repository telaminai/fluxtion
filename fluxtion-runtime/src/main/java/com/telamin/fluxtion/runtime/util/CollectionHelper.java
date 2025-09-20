/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.util;

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
