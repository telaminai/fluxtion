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
import com.telamin.fluxtion.runtime.flowfunction.Tuple;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableBiFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import lombok.Value;

public class Tuples {

    public static <F, S, TIN extends Tuple<? extends F, ? extends S>> SerializableFunction<TIN, Tuple<F, S>>
    replaceNull(F first, S second) {
        return new ReplaceNull<>(first, second)::replaceNull;
    }

    public static <F, S, R, T extends Tuple<F, S>> SerializableFunction<T, R>
    mapTuple(SerializableBiFunction<F, S, R> tupleMapFunction) {
        return new MapTuple<>(tupleMapFunction)::mapTuple;
    }


    public static class ReplaceNull<F, S> {
        private final F firstValue;
        private final S secondValue;

        public ReplaceNull(
                @AssignToField("firstValue")
                F firstValue,
                @AssignToField("secondValue")
                S secondValue) {
            this.firstValue = firstValue;
            this.secondValue = secondValue;
        }

        public Tuple<F, S> replaceNull(Tuple<? extends F, ? extends S> in) {
            F first = in.getFirst() == null ? firstValue : in.getFirst();
            S second = in.getSecond() == null ? secondValue : in.getSecond();
            return Tuple.build(first, second);
        }
    }

    @Value
    public static class MapTuple<F, S, R> {
        SerializableBiFunction<F, S, R> tupleMapFunction;

        public R mapTuple(Tuple<? extends F, ? extends S> tuple) {
            return tupleMapFunction.apply(tuple.getFirst(), tuple.getSecond());
        }

    }
}
