/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.ml;

import com.fluxtion.dataflow.runtime.annotations.feature.Experimental;

@Experimental
public class MutableDouble {
    double value;

    public MutableDouble(double value) {
        this.value = value;
    }

    public MutableDouble() {
        this(Double.NaN);
    }

    void reset() {
        value = Double.NaN;
    }
}
