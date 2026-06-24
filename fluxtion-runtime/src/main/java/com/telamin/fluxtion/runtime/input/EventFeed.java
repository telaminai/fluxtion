/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.input;

import com.telamin.fluxtion.runtime.DataFlow;

public interface EventFeed<T> {

    void registerSubscriber(DataFlow subscriber);

    void subscribe(DataFlow subscriber, T subscriptionId);

    void unSubscribe(DataFlow subscriber, T subscriptionId);

    void removeAllSubscriptions(DataFlow subscriber);
}
