/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.ml;

import com.telamin.fluxtion.runtime.annotations.NoPropagateFunction;

import java.util.List;

/**
 * Accepts {@link Calibration} messages for processing and applying to features/models.
 */
public interface CalibrationProcessor {

    /**
     * A list of calibrations that are to be applied to the features. A calibration has a featureIdentifier field
     * that receivers can use to filter the incoming calibration and apply as necessary.
     *
     * @param calibration list of calibrations to apply
     * @return change notification for propagation
     */
    boolean setCalibration(List<Calibration> calibration);

    /**
     * Reset all calibrations to 1
     *
     * @return change notification for propagation
     */
    @NoPropagateFunction
    default boolean resetToOne() {
        return false;
    }

    /**
     * Reset all calibrations to o
     *
     * @return change notification for propagation
     */
    @NoPropagateFunction
    default boolean resetToZero() {
        return false;
    }
}
