/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction;

import java.util.function.IntSupplier;

/**
 * Makes the output of a {@link IntFlowFunction} available in a user class
 */
public interface IntFlowSupplier extends FlowSupplier<Integer>, IntSupplier {
    default Integer get() {
        return getAsInt();
    }
}
