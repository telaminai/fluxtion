/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.callback;

import java.util.Iterator;

public interface CallbackDispatcher extends EventDispatcher {
    String DEFAULT_NODE_NAME = "callbackDispatcher";

    void fireCallback(int id);

    <T> void fireCallback(int id, T item);

    <R> void fireIteratorCallback(int callbackId, Iterator<R> dataIterator);
}
