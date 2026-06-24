/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction;

import java.util.function.DoubleSupplier;

/**
 * A primitive double function step applied to a data flow.
 */
public interface DoubleFlowFunction extends FlowFunction<Double>, DoubleSupplier, DoubleFlowSupplier {
    default Double get() {
        return getAsDouble();
    }
}
