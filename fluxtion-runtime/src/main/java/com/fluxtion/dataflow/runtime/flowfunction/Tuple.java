/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction;

import com.fluxtion.dataflow.runtime.flowfunction.groupby.MutableTuple;

public interface Tuple<F, S> {
    static <F, S> Tuple<F, S> build(F first, S second) {
        return new MutableTuple<>(first, second);
    }

    F getFirst();

    S getSecond();
}
