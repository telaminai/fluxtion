/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction;

import java.util.function.DoubleSupplier;

/**
 * Makes the output of a {@link DoubleFlowFunction} available in a user class
 */
public interface DoubleFlowSupplier extends FlowSupplier<Double>, DoubleSupplier {
    default Double get() {
        return getAsDouble();
    }
}
