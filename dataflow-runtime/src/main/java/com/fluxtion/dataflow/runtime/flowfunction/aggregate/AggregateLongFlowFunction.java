/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.aggregate;

import java.util.function.LongSupplier;

public interface AggregateLongFlowFunction<T extends AggregateLongFlowFunction<T>>
        extends AggregateFlowFunction<Long, Long, T>, LongSupplier {
    long resetLong();

    long aggregateLong(long input);
}
