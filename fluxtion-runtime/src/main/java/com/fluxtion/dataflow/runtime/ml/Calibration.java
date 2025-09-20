/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.ml;

import com.fluxtion.dataflow.runtime.annotations.feature.Experimental;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Experimental
public class Calibration {
    private String featureIdentifier;
    private Class<? extends Feature> featureClass;
    private int featureVersion;
    private double co_efficient;
    private double weight;

    public String getFeatureIdentifier() {
        return featureIdentifier == null ? featureClass.getSimpleName() : featureIdentifier;
    }
}
