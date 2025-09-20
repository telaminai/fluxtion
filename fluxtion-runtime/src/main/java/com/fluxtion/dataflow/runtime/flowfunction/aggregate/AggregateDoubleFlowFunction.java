/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate;

import java.util.function.DoubleSupplier;

public interface AggregateDoubleFlowFunction<T extends AggregateDoubleFlowFunction<T>>
        extends AggregateFlowFunction<Double, Double, T>, DoubleSupplier {


    double resetDouble();

    double aggregateDouble(double input);
}
