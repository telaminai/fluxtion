/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.serializer;

import java.util.HashMap;
import java.util.Map;

public class MapBuilder {

    private MapBuilder() {
    }

    public static MapBuilder builder() {
        return new MapBuilder();
    }

    private final HashMap map = new HashMap();

    public <K, V> Map<K, V> build() {
        return map;
    }

    public MapBuilder put(Object key, Object value) {
        map.put(key, value);
        return this;
    }
}
