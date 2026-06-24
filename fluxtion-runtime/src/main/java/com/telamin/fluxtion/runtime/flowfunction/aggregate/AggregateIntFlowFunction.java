/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate;

import java.util.function.IntSupplier;

public interface AggregateIntFlowFunction<T extends AggregateIntFlowFunction<T>>
        extends AggregateFlowFunction<Integer, Integer, T>, IntSupplier {
    int resetInt();

    int aggregateInt(int input);
}
