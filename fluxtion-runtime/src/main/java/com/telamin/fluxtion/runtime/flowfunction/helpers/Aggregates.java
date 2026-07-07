/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
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

    /**
     * Average over any {@link Number} input returning an exact {@code Double} — the right
     * choice for SQL {@code AVG} over int/long/double (no lossy integer division). The
     * natural component accessor binds without a widening step.
     */
    public static SerializableSupplier<NumberAverageFlowFunction> numberAverageFactory() {
        return NumberAverageFlowFunction::new;
    }
}
