/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.helpers;

import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.AggregateIdentityFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.AggregateToListFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupByFlowFunctionWrapper;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;

import java.util.List;

public class GroupingFactory<T, K, O, F extends AggregateFlowFunction<T, O, F>> {
    private final SerializableFunction<T, K> keyFunction;
    private final SerializableSupplier<F> aggregateFunctionSupplier;

    public GroupingFactory(SerializableFunction<T, K> keyFunction) {
        this.keyFunction = keyFunction;
        this.aggregateFunctionSupplier = null;
    }

    public GroupingFactory(
            @AssignToField("keyFunction") SerializableFunction<T, K> keyFunction,
            @AssignToField("aggregateFunctionSupplier") SerializableSupplier<F> aggregateFunctionSupplier) {
        this.keyFunction = keyFunction;
        this.aggregateFunctionSupplier = aggregateFunctionSupplier;
    }

    public SerializableFunction<T, K> getKeyFunction() {
        return keyFunction;
    }

    public GroupByFlowFunctionWrapper<T, K, T, List<T>, AggregateToListFlowFunction<T>> groupByToList() {
        SerializableSupplier<AggregateToListFlowFunction<T>> list = Collectors.listFactory();
        return new GroupByFlowFunctionWrapper<>(keyFunction, Mappers::identity, list);
    }

    public GroupByFlowFunctionWrapper<T, K, T, T, AggregateIdentityFlowFunction<T>> groupBy() {
        SerializableSupplier<AggregateIdentityFlowFunction<T>> aggregateIdentityFlowFunctionSerializableSupplier = Aggregates.identityFactory();
        return new GroupByFlowFunctionWrapper<>(keyFunction, Mappers::identity, aggregateIdentityFlowFunctionSerializableSupplier);
    }

    public GroupByFlowFunctionWrapper<T, K, T, O, F> groupingByXXX() {
        return new GroupByFlowFunctionWrapper<>(keyFunction, Mappers::identity, aggregateFunctionSupplier);
    }
}
