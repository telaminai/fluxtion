/*
 * SPDX-File Copyright: © 2019-2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.node;

import com.fluxtion.dataflow.runtime.event.NamedFeedEvent;

import java.util.Map;

public interface TableNode<K, V> {
    Map<K, V> getTableMap();

    <T> NamedFeedEvent<T> getLastFeedEvent();
}
