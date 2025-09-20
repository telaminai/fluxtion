/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate.function;

import com.fluxtion.dataflow.runtime.flowfunction.aggregate.AggregateFlowFunction;

import java.util.HashSet;
import java.util.Set;

public class AggregateToSetFlowFunction<T> implements AggregateFlowFunction<T, Set<T>, AggregateToSetFlowFunction<T>> {

    private transient final Set<T> list = new HashSet<>();

    @Override
    public Set<T> reset() {
        list.clear();
        return list;
    }

    @Override
    public void combine(AggregateToSetFlowFunction<T> add) {
        list.addAll(add.list);
    }

    @Override
    public void deduct(AggregateToSetFlowFunction<T> add) {
        list.removeAll(add.list);
    }

    @Override
    public Set<T> get() {
        return list;
    }

    @Override
    public Set<T> aggregate(T input) {
        list.add(input);
        return list;
    }

}
