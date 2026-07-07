/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function;

import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;

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

    /**
     * Non-invertible: a {@link HashSet} carries no multiplicity, so {@code removeAll} is not the
     * inverse of {@code addAll} — a value present in both an expiring and a live bucket would be
     * dropped. Forcing {@code false} routes sliding windows onto the full-recompute branch.
     */
    @Override
    public boolean deductSupported() {
        return false;
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
