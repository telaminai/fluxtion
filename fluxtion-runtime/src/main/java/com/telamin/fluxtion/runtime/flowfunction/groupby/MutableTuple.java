/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.flowfunction.Tuple;
import com.telamin.fluxtion.runtime.util.ObjectPool;
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
