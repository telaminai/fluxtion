/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.flowfunction;

import com.telamin.fluxtion.runtime.context.buildtime.GeneratorNodeCollection;
import com.telamin.fluxtion.runtime.flowfunction.groupby.MultiJoin;
import com.telamin.fluxtion.runtime.partition.LambdaReflection;
import lombok.Data;

/**
 * Builds a co-group or multi leg join
 *
 * @param <K> The key type for join stream
 * @param <T> Target type of results for multi join
 */
public class MultiJoinBuilder<K, T> {

    private final MultiJoin<K, T> multiLegJoin;

    /**
     * Builds a GroupByFlowBuilder that is formed from multiple joins and pushed to a target instance.
     *
     * @param target   Supplier of target instances that store the result of the join
     * @param joinLegs The legs that supply the inputs to the join
     * @param <K>      The key class
     * @param <T>      The join target class
     * @return The GroupByFlow with a new instance of the target allocated to every key
     */
    @SuppressWarnings("all")
    public static <K, T> GroupByFlowBuilder<K, T> multiJoin(LambdaReflection.SerializableSupplier<T> target, MultiJoinLeg<K, T, ?>... joinLegs) {
        MultiJoinBuilder multiJoinBuilder = new MultiJoinBuilder(Object.class, target);
        for (MultiJoinLeg joinLeg : joinLegs) {
            multiJoinBuilder.addJoin(joinLeg.flow, joinLeg.setter);
        }
        return multiJoinBuilder.dataFlow();
    }

    public static <K, T> MultiJoinBuilder<K, T> builder(Class<K> keyClass, LambdaReflection.SerializableSupplier<T> target) {
        return new MultiJoinBuilder<>(keyClass, target);
    }

    public MultiJoinBuilder(Class<K> keyClass, LambdaReflection.SerializableSupplier<T> target) {
        multiLegJoin = new MultiJoin<>(keyClass, target);
    }

    public static <K1, T1, R> MultiJoinLeg<K1, T1, R> multiJoinLeg(GroupByFlowBuilder<K1, R> flow, LambdaReflection.SerializableBiConsumer<T1, R> setter) {
        return new MultiJoinLeg<>(flow, setter);
    }

    @Data
    public static class MultiJoinLeg<K, T, R> {
        private final GroupByFlowBuilder<K, R> flow;
        private final LambdaReflection.SerializableBiConsumer<T, R> setter;
    }

    public <K2 extends K, B> MultiJoinBuilder<K, T> addJoin(
            GroupByFlowBuilder<K2, B> flow1,
            LambdaReflection.SerializableBiConsumer<T, B> setter1) {
        multiLegJoin.addJoin(flow1.flowSupplier(), setter1);
        return this;
    }

    public <K2 extends K, B> MultiJoinBuilder<K, T> addOptionalJoin(
            GroupByFlowBuilder<K2, B> flow1,
            LambdaReflection.SerializableBiConsumer<T, B> setter1) {
        multiLegJoin.addOptionalJoin(flow1.flowSupplier(), setter1);
        return this;
    }

    public GroupByFlowBuilder<K, T> dataFlow() {
        GeneratorNodeCollection.service().add(multiLegJoin);
        return new GroupByFlowBuilder<>(multiLegJoin);
    }
}
