/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.flowfunction.Tuple;
import com.telamin.fluxtion.runtime.util.ObjectPool;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class MutableTuple<F, S> implements Tuple<F, S>, java.io.Serializable {
    private static final long serialVersionUID = 1L;
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
