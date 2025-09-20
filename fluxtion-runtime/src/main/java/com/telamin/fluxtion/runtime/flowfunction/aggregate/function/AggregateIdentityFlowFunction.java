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
