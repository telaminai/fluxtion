/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function;

import com.fluxtion.dataflow.runtime.flowfunction.aggregate.AggregateFlowFunction;

public class AggregateIdentityFlowFunction<T> implements AggregateFlowFunction<T, T, AggregateIdentityFlowFunction<T>> {
    T value;

    @Override
    public T reset() {
        value = null;
        return null;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public T aggregate(T input) {
        value = input;
        return value;
    }
}
