/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.ml;

import com.telamin.fluxtion.runtime.annotations.feature.Experimental;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Experimental
/**
 * Calibration message used to tune feature contribution at runtime.
 *
 * <p>Targeting:
 * - If {@code featureIdentifier} is non-null, it is used to select the target feature(s).
 * - Otherwise, if {@code featureClass} is provided, the target identifier defaults to {@code featureClass.getSimpleName()}.
 *
 * <p>Versioning:
 * - Receivers may implement version gating; the provided runtime does so in {@link AbstractFeature}, only
 *   applying calibrations whose {@code featureVersion} is greater than or equal to the last applied version.
 */
public class Calibration {
    /** explicit feature identifier to target; if null, {@link #featureClass} simple name is used */
    private String featureIdentifier;
    /** optional class reference to derive default identifier when {@link #featureIdentifier} is null */
    private Class<? extends Feature> featureClass;
    /** optional monotonically increasing version for out-of-order delivery handling */
    private int featureVersion;
    /** trained model coefficient or operational override */
    private double co_efficient;
    /** runtime calibration multiplier */
    private double weight;

    /** resolve the effective target identifier */
    public String getFeatureIdentifier() {
        return featureIdentifier == null ? featureClass.getSimpleName() : featureIdentifier;
    }
}
