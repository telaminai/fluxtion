/*
 * SPDX-File Copyright: © 2024-2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.input;

import com.fluxtion.dataflow.runtime.event.NamedFeedEvent;
import com.fluxtion.dataflow.runtime.node.EventSubscription;

public interface NamedFeed<T> extends EventFeed<EventSubscription<?>> {
    NamedFeedEvent<?>[] EMPTY_ARRAY = new NamedFeedEvent[0];

    @SuppressWarnings({"raw", "unchecked"})
    default  NamedFeedEvent<?>[] eventLog() {
        return  EMPTY_ARRAY;
    }

    String getFeedName();
}