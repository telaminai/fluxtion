/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.ml;

import com.fluxtion.dataflow.runtime.annotations.feature.Experimental;

import java.util.List;

@Experimental
public interface PredictiveModel {

    double predictedValue();

    List<Feature> features();
}
