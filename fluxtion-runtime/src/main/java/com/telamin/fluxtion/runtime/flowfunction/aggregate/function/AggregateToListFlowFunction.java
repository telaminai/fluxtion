/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function;

import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;

import java.util.ArrayList;
import java.util.List;

public class AggregateToListFlowFunction<T> implements AggregateFlowFunction<T, List<T>, AggregateToListFlowFunction<T>> {

    private transient final List<T> list = new ArrayList<>();
    private final int maxElementCount;


    public AggregateToListFlowFunction() {
        this(-1);
    }

    public AggregateToListFlowFunction(int maxElementCount) {
        this.maxElementCount = maxElementCount;
    }

    @Override
    public List<T> reset() {
        list.clear();
        return list;
    }

    @Override
    public void combine(AggregateToListFlowFunction<T> add) {
        list.addAll(add.list);
        while (maxElementCount > 0 & list.size() > maxElementCount) {
            list.remove(0);
        }
    }

    @Override
    public void deduct(AggregateToListFlowFunction<T> add) {
        list.removeAll(add.list);
    }

    @Override
    public List<T> get() {
        return list;
    }

    @Override
    public List<T> aggregate(T input) {
        list.add(input);
        if (maxElementCount > 0 & list.size() > maxElementCount) {
            list.remove(0);
        }
        return list;
    }


    public static class AggregateToListFactory {
        private final int maxElementCount;

        public AggregateToListFactory(int maxElementCount) {
            this.maxElementCount = maxElementCount;
        }

        public <T> AggregateToListFlowFunction<T> newList() {
            return new AggregateToListFlowFunction<>(maxElementCount);
        }
    }
}
