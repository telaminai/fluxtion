/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction;

import java.util.function.DoubleSupplier;

/**
 * A primitive double function step applied to a data flow.
 */
public interface DoubleFlowFunction extends FlowFunction<Double>, DoubleSupplier, DoubleFlowSupplier {
    default Double get() {
        return getAsDouble();
    }
}
