/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.output;

import com.telamin.fluxtion.runtime.CloneableDataFlow;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A sink for an CloneableDataFlow. Implement this interface and register with :
 * <p>
 * {@link CloneableDataFlow#registerService(Object, Class, String)}
 *
 * @param <T> the type of message published to the Sink
 */
public interface MessageSink<T> extends Consumer<T> {

    default void setValueMapper(Function<? super T, ?> valueMapper) {
        throw new UnsupportedOperationException("setValueMapper not implemented");
    }
}