/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.helpers;

import com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.AggregateToListFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.AggregateToListFlowFunction.AggregateToListFactory;
import com.fluxtion.dataflow.runtime.flowfunction.aggregate.function.AggregateToSetFlowFunction;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableFunction;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableSupplier;

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
