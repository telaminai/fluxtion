/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.helpers;

import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.AggregateToListFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.AggregateToListFlowFunction.AggregateToListFactory;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.AggregateToSetFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface Collectors {

    static <T> SerializableFunction<T, Set<T>> toSet() {
        return new AggregateToSetFlowFunction<T>()::aggregate;
    }

    static <T> SerializableFunction<T, List<T>> toList() {
        return new AggregateToListFlowFunction<T>()::aggregate;
    }

    static <T> SerializableFunction<T, Collection<T>> toCollection() {
        return new AggregateToListFlowFunction<T>()::aggregate;
    }

    static <T> SerializableFunction<T, List<T>> toList(int maxElements) {
        return new AggregateToListFlowFunction<T>(maxElements)::aggregate;
    }

    static <T> SerializableSupplier<AggregateToListFlowFunction<T>> listFactory(int maximumElementCount) {
        return new AggregateToListFactory(maximumElementCount)::newList;
    }

    static <T> SerializableSupplier<AggregateToListFlowFunction<T>> listFactory() {
        return listFactory(-1);
    }

    static <T> SerializableSupplier<AggregateToSetFlowFunction<T>> setFactory() {
        return AggregateToSetFlowFunction::new;
    }
}
