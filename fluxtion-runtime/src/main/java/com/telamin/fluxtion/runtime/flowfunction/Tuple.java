/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction;

import com.telamin.fluxtion.runtime.flowfunction.groupby.MutableTuple;

public interface Tuple<F, S> {
    static <F, S> Tuple<F, S> build(F first, S second) {
        return new MutableTuple<>(first, second);
    }

    F getFirst();

    S getSecond();
}
