/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.ml;

import com.fluxtion.dataflow.runtime.annotations.ExportService;
import com.fluxtion.dataflow.runtime.annotations.Initialise;
import com.fluxtion.dataflow.runtime.annotations.NoPropagateFunction;
import com.fluxtion.dataflow.runtime.annotations.OnTrigger;
import com.fluxtion.dataflow.runtime.annotations.feature.Experimental;
import com.fluxtion.dataflow.runtime.util.CollectionHelper;

import java.util.Arrays;
import java.util.List;

@Experimental
public class PredictiveLinearRegressionModel implements PredictiveModel, @ExportService CalibrationProcessor {

    private final Feature[] features;
    private final transient List<Feature> immutableFeatures;
    private double prediction = Double.NaN;

    public PredictiveLinearRegressionModel(Feature... features) {
        this.features = Arrays.copyOf(features, features.length);
        immutableFeatures = CollectionHelper.listOf(features);
    }

    public PredictiveLinearRegressionModel(List<Feature> featureList) {
        this(featureList.toArray(new Feature[0]));
    }

    @Initialise
    public void init() {
        prediction = Double.NaN;
    }

    @Override
    @NoPropagateFunction
    public boolean setCalibration(List<Calibration> calibrations) {
        return calculateInference();
    }

    @OnTrigger
    public boolean calculateInference() {
        prediction = 0;
        for (Feature feature : features) {
            prediction += feature.value();
        }
        return true;
    }

    @Override
    public double predictedValue() {
        return prediction;
    }

    @Override
    public List<Feature> features() {
        return immutableFeatures;
    }
}
