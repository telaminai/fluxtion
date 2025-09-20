/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate;

import com.telamin.fluxtion.runtime.flowfunction.Stateful;

import java.util.function.Supplier;

/**
 * {@link java.util.function.Function}
 *
 * @param <I> Input type
 * @param <R> Return type of the wrapped function
 * @param <T> The type of this BaseSlidingWindowFunction
 */
public interface AggregateFlowFunction<I, R, T extends AggregateFlowFunction<I, R, T>> extends Stateful<R>, Supplier<R> {

    default void combine(T add) {
        throw new RuntimeException("Sliding not supported implement combine for " + this.getClass().getName());
    }

    default void deduct(T add) {
        throw new RuntimeException("Sliding not supported implement deduct for " + this.getClass().getName());
    }

    default boolean deductSupported() {
        return true;
    }

    R get();

    R aggregate(I input);

}
