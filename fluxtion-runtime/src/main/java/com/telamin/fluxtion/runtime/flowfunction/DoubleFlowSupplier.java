/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction;

import java.util.function.DoubleSupplier;

/**
 * Makes the output of a {@link DoubleFlowFunction} available in a user class
 */
public interface DoubleFlowSupplier extends FlowSupplier<Double>, DoubleSupplier {
    default Double get() {
        return getAsDouble();
    }
}
