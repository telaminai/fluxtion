/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.builder.flowfunction;

import com.telamin.fluxtion.runtime.flowfunction.Tuple;
import com.telamin.fluxtion.runtime.flowfunction.groupby.InnerJoin;
import com.telamin.fluxtion.runtime.flowfunction.groupby.LeftJoin;
import com.telamin.fluxtion.runtime.flowfunction.groupby.OuterJoin;
import com.telamin.fluxtion.runtime.flowfunction.groupby.RightJoin;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Tuples;
import com.telamin.fluxtion.runtime.partition.LambdaReflection;

public interface JoinFlowBuilder {
    static <K1, V1, K2 extends K1, V2> GroupByFlowBuilder<K1, Tuple<V1, V2>> innerJoin(
            GroupByFlowBuilder<K1, V1> leftGroupBy,
            GroupByFlowBuilder<K2, V2> rightGroupBy) {
        return leftGroupBy.mapBiFunction(new InnerJoin()::join, rightGroupBy);
    }

    static <K1, V1, K2 extends K1, V2, R> GroupByFlowBuilder<K1, R> innerJoin(
            GroupByFlowBuilder<K1, V1> leftGroupBy,
            GroupByFlowBuilder<K2, V2> rightGroupBy,
            LambdaReflection.SerializableBiFunction<V1, V2, R> mergeFunction) {
        return leftGroupBy.mapBiFunction(new InnerJoin()::join, rightGroupBy).mapValues(Tuples.mapTuple(mergeFunction));
    }

    static <K1, V1, K2 extends K1, V2> GroupByFlowBuilder<K1, Tuple<V1, V2>> outerJoin(
            GroupByFlowBuilder<K1, V1> leftGroupBy,
            GroupByFlowBuilder<K2, V2> rightGroupBy) {
        return leftGroupBy.mapBiFunction(new OuterJoin()::join, rightGroupBy);
    }

    static <K1, V1, K2 extends K1, V2, R> GroupByFlowBuilder<K1, R> outerJoin(
            GroupByFlowBuilder<K1, V1> leftGroupBy,
            GroupByFlowBuilder<K2, V2> rightGroupBy,
            LambdaReflection.SerializableBiFunction<V1, V2, R> mergeFunction) {
        return leftGroupBy.mapBiFunction(new OuterJoin()::join, rightGroupBy).mapValues(Tuples.mapTuple(mergeFunction));
    }

    static <K1, V1, K2 extends K1, V2> GroupByFlowBuilder<K1, Tuple<V1, V2>> leftJoin(
            GroupByFlowBuilder<K1, V1> leftGroupBy,
            GroupByFlowBuilder<K2, V2> rightGroupBy) {
        return leftGroupBy.mapBiFunction(new LeftJoin()::join, rightGroupBy);
    }

    static <K1, V1, K2 extends K1, V2, R> GroupByFlowBuilder<K1, R> leftJoin(
            GroupByFlowBuilder<K1, V1> leftGroupBy,
            GroupByFlowBuilder<K2, V2> rightGroupBy,
            LambdaReflection.SerializableBiFunction<V1, V2, R> mergeFunction) {
        return leftGroupBy.mapBiFunction(new LeftJoin()::join, rightGroupBy).mapValues(Tuples.mapTuple(mergeFunction));
    }

    static <K1, V1, K2 extends K1, V2> GroupByFlowBuilder<K1, Tuple<V1, V2>> rightJoin(
            GroupByFlowBuilder<K1, V1> leftGroupBy,
            GroupByFlowBuilder<K2, V2> rightGroupBy) {
        return leftGroupBy.mapBiFunction(new RightJoin()::join, rightGroupBy);
    }

    static <K1, V1, K2 extends K1, V2, R> GroupByFlowBuilder<K1, R> rightJoin(
            GroupByFlowBuilder<K1, V1> leftGroupBy,
            GroupByFlowBuilder<K2, V2> rightGroupBy,
            LambdaReflection.SerializableBiFunction<V1, V2, R> mergeFunction) {
        return leftGroupBy.mapBiFunction(new RightJoin()::join, rightGroupBy).mapValues(Tuples.mapTuple(mergeFunction));
    }
}
