/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction;

import java.util.function.Supplier;

/**
 * A function step applied to a data flow.
 *
 * @param <R>
 */
public interface FlowFunction<R> extends Supplier<R>, ParallelFunction {

    default boolean hasDefaultValue() {
        return false;
    }

}
