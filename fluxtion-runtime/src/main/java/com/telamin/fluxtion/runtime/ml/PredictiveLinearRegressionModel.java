/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.ml;

import com.telamin.fluxtion.runtime.annotations.ExportService;
import com.telamin.fluxtion.runtime.annotations.Initialise;
import com.telamin.fluxtion.runtime.annotations.NoPropagateFunction;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.annotations.feature.Experimental;
import com.telamin.fluxtion.runtime.util.CollectionHelper;

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
        // Note: Each Feature also implements CalibrationProcessor and will receive this message independently.
        // This method simply recomputes the model prediction after features adjust their internal state.
        return calculateInference();
    }

    @Override
    @NoPropagateFunction
    public boolean resetToOne() {
        // The reset message is broadcast to all CalibrationProcessor implementors (features included).
        // We recalculate to reflect immediate changes without requiring a new data event.
        return calculateInference();
    }

    @Override
    @NoPropagateFunction
    public boolean resetToZero() {
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
