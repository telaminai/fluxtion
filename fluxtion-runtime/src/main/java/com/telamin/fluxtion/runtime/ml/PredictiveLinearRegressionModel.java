/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
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
