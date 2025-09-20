/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.helpers;

import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.AggregateIdentityFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive.*;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;

public class Aggregates {

    public static <T> SerializableSupplier<AggregateIdentityFlowFunction<T>> identityFactory() {
        return AggregateIdentityFlowFunction::new;
    }

    public static SerializableSupplier<IntIdentityFlowFunction> intIdentityFactory() {
        return IntIdentityFlowFunction::new;
    }

    public static SerializableSupplier<DoubleIdentityFlowFunction> doubleIdentityFactory() {
        return DoubleIdentityFlowFunction::new;
    }

    public static SerializableSupplier<LongIdentityFlowFunction> longIdentityFactory() {
        return LongIdentityFlowFunction::new;
    }

    public static <T> SerializableSupplier<CountFlowFunction<T>> countFactory() {
        return CountFlowFunction::new;
    }

    //SUM
    public static SerializableSupplier<IntSumFlowFunction> intSumFactory() {
        return IntSumFlowFunction::new;
    }

    public static SerializableSupplier<DoubleSumFlowFunction> doubleSumFactory() {
        return DoubleSumFlowFunction::new;
    }

    public static SerializableSupplier<LongSumFlowFunction> longSumFactory() {
        return LongSumFlowFunction::new;
    }

    //max
    public static SerializableSupplier<IntMaxFlowFunction> intMaxFactory() {
        return IntMaxFlowFunction::new;
    }

    public static SerializableSupplier<LongMaxFlowFunction> longMaxFactory() {
        return LongMaxFlowFunction::new;
    }

    public static SerializableSupplier<DoubleMaxFlowFunction> doubleMaxFactory() {
        return DoubleMaxFlowFunction::new;
    }

    //min
    public static SerializableSupplier<IntMinFlowFunction> intMinFactory() {
        return IntMinFlowFunction::new;
    }

    public static SerializableSupplier<LongMinFlowFunction> longMinFactory() {
        return LongMinFlowFunction::new;
    }

    public static SerializableSupplier<DoubleMinFlowFunction> doubleMinFactory() {
        return DoubleMinFlowFunction::new;
    }

    //AVERAGE
    public static SerializableSupplier<IntAverageFlowFunction> intAverageFactory() {
        return IntAverageFlowFunction::new;
    }

    public static SerializableSupplier<DoubleAverageFlowFunction> doubleAverageFactory() {
        return DoubleAverageFlowFunction::new;
    }

    public static SerializableSupplier<LongAverageFlowFunction> longAverageFactory() {
        return LongAverageFlowFunction::new;
    }
}
