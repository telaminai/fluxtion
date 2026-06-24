/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.node;

import com.telamin.fluxtion.runtime.event.NamedFeedEvent;

import java.util.Map;

public interface TableNode<K, V> {
    Map<K, V> getTableMap();

    <T> NamedFeedEvent<T> getLastFeedEvent();
}
