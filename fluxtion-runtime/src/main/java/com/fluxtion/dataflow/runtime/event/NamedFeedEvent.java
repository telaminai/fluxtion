/*
 * SPDX-File Copyright: © 2019-2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.event;

public interface NamedFeedEvent<T> extends Event {
    String eventFeedName();

    String topic();

    T data();

    long sequenceNumber();

    boolean delete();
}
