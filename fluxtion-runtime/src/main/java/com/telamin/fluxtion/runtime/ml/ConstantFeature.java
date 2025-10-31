/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.ml;

import com.telamin.fluxtion.runtime.annotations.Initialise;
import com.telamin.fluxtion.runtime.annotations.NoPropagateFunction;
import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.annotations.feature.Experimental;

import java.util.List;

/**
 * A constant-valued feature that always contributes (1.0 * co_efficient * weight) to the model.
 *
 * <p>Use this as an intercept (bias) term in a linear or multiple linear regression. Typical usage:
 * <pre>
 *     PredictiveLinearRegressionModel model = new PredictiveLinearRegressionModel(
 *             new ConstantFeature(),
 *             Feature.include(flow, Order::getQty).get(0)
 *     );
 *     // Calibrate the bias to 5.0
 *     sep.getExportedService(CalibrationProcessor.class).setCalibration(List.of(
 *         Calibration.builder().featureClass(ConstantFeature.class).co_efficient(5.0).weight(1.0).build()
 *     ));
 * </pre>
 *
 * <p>Calibration messages that target this feature cause immediate recomputation because a constant raw value of 1.0
 * is always available.
 */
@Experimental
public class ConstantFeature extends AbstractFeature {

    public ConstantFeature() {
        super();
    }

    public ConstantFeature(
            @AssignToField("name") String name,
            @AssignToField("identifier") String identifier) {
        super(name, identifier);
    }

    /**
     * Ensure a raw value of 1.0 is available from the start so calibrations and resets immediately affect the model.
     */
    @Initialise
    public void initConstant() {
        // AbstractFeature.init() will set baseline values; calling updateFromRaw afterwards is safe and idempotent.
        updateFromRaw(1.0);
    }

    /**
     * Apply calibration to this constant feature and recompute immediately using raw=1.0.
     */
    @Override
    @NoPropagateFunction
    public boolean setCalibration(List<Calibration> calibrations) {
        boolean applied = super.setCalibration(calibrations);
        if (applied) {
            // Guarantee immediate recompute even if init path did not mark raw as present yet
            if (!hasRaw) {
                updateFromRaw(1.0);
            } else {
                // hasRaw true implies super.setCalibration already recomputed using lastRaw
            }
            return true;
        }
        return false;
    }
}
