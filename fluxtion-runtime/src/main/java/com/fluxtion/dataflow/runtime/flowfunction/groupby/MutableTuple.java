/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.groupby;

import com.fluxtion.dataflow.runtime.flowfunction.Tuple;
import com.fluxtion.dataflow.runtime.util.ObjectPool;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class MutableTuple<F, S> implements Tuple<F, S> {
    private F first;
    private S second;

    public MutableTuple(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public MutableTuple() {
    }

    @Override
    public F getFirst() {
        return first;
    }

    @Override
    public S getSecond() {
        return second;
    }

    public MutableTuple<F, S> setFirst(F first) {
        this.first = first;
        return this;
    }

    public MutableTuple<F, S> setSecond(S second) {
        this.second = second;
        return this;
    }

    public void returnToPool(ObjectPool objectPool) {
        setFirst(null);
        setSecond(null);
        objectPool.checkIn(this);
    }
}
