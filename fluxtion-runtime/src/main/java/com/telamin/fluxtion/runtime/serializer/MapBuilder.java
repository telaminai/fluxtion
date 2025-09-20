/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
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
