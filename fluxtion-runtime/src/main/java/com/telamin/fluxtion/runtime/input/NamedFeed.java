/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.input;

import com.telamin.fluxtion.runtime.event.NamedFeedEvent;
import com.telamin.fluxtion.runtime.node.EventSubscription;

public interface NamedFeed<T> extends EventFeed<EventSubscription<?>> {
    NamedFeedEvent<?>[] EMPTY_ARRAY = new NamedFeedEvent[0];

    @SuppressWarnings({"raw", "unchecked"})
    default  NamedFeedEvent<?>[] eventLog() {
        return  EMPTY_ARRAY;
    }

    String getFeedName();
}