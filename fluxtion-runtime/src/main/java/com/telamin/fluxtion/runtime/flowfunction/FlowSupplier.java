/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction;

import java.util.function.Supplier;

/**
 * Makes the output of a {@link FlowFunction} available in a user class
 *
 * @param <R>
 */
public interface FlowSupplier<R> extends Supplier<R> {
    boolean hasChanged();

}
