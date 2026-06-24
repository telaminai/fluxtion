/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.event;

public interface NamedFeedEvent<T> extends Event {
    String eventFeedName();

    String topic();

    T data();

    long sequenceNumber();

    boolean delete();
}
