/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction;

import java.util.function.LongSupplier;

/**
 * Makes the output of a {@link LongFlowFunction} available in a user class
 */
public interface LongFlowSupplier extends FlowSupplier<Long>, LongSupplier {
    default Long get() {
        return getAsLong();
    }
}
