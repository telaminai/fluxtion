/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction;

import java.util.function.IntSupplier;

/**
 * A primitive int function step applied to a data flow.
 */
public interface IntFlowFunction extends FlowFunction<Integer>, IntSupplier, IntFlowSupplier {
    default Integer get() {
        return getAsInt();
    }
}
