/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.input;

import com.telamin.fluxtion.runtime.node.EventSubscription;

public interface SubscriptionManager {
    String DEFAULT_NODE_NAME = "subscriptionManager";

    void subscribe(Object subscriptionId);

    void unSubscribe(Object subscriptionId);

    void subscribeToNamedFeed(EventSubscription<?> subscription);

    void subscribeToNamedFeed(String feedName);

    void unSubscribeToNamedFeed(EventSubscription<?> subscription);

    void unSubscribeToNamedFeed(String feedName);
}
