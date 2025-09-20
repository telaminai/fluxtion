/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
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