/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction;

import java.util.function.LongSupplier;

/**
 * Makes the output of a {@link LongFlowFunction} available in a user class
 */
public interface LongFlowSupplier extends FlowSupplier<Long>, LongSupplier {
    default Long get() {
        return getAsLong();
    }
}
