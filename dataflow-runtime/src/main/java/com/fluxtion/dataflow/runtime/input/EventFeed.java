/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.input;

import com.fluxtion.dataflow.runtime.DataFlow;

public interface EventFeed<T> {

    void registerSubscriber(DataFlow subscriber);

    void subscribe(DataFlow subscriber, T subscriptionId);

    void unSubscribe(DataFlow subscriber, T subscriptionId);

    void removeAllSubscriptions(DataFlow subscriber);
}
