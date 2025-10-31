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
import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.annotations.feature.Experimental;

import java.beans.Introspector;
import java.util.List;

@Experimental
public abstract class AbstractFeature implements Feature, @ExportService CalibrationProcessor {

    /**
     * Human-friendly node name for generated sources and logging.
     */
    private final transient String name;
    /**
     * Stable feature identifier used by Calibration messages to target this feature.
     */
    private final transient String identifier;
    /**
     * Trained coefficient for this feature (β). This is typically set during model training
     * and may be overridden by a Calibration for operational reasons.
     */
    protected double co_efficient;
    /**
     * Runtime calibration weight (w). Multiplied with the trained coefficient to scale the raw signal.
     */
    protected double weight;
    /**
     * Current contribution of this feature to the model (raw * β * w).
     */
    protected double value;
    /**
     * Last raw signal value used to compute this feature. Enables immediate recomputation when
     * calibration is changed or resets are applied, without waiting for the next event.
     */
    protected double lastRaw;
    /**
     * Flag marking whether a raw value has been observed yet.
     */
    protected boolean hasRaw;
    /**
     * Version of the last applied calibration. Newer or equal versions will be accepted.
     */
    protected int currentVersion;

    public AbstractFeature() {
        identifier = getClass().getSimpleName();
        name = Introspector.decapitalize(identifier);
    }

    public AbstractFeature(
            @AssignToField("name") String name,
            @AssignToField("identifier") String identifier) {
        this.name = name;
        this.identifier = identifier;
    }

    /**
     * Initialise default state. Features start with zero contribution and no raw value observed.
     */
    @Initialise
    public void init() {
        co_efficient = 0;
        weight = 0;
        value = 0;
        lastRaw = Double.NaN;
        hasRaw = false;
        currentVersion = Integer.MIN_VALUE;
    }

    /**
     * Apply incoming calibration if it targets this feature. When applied, and if a raw value has
     * already been observed, the contribution is recomputed immediately using the latest raw value.
     */
    @Override
    @NoPropagateFunction
    public boolean setCalibration(List<Calibration> calibrations) {
        for (int i = 0, calibrationsSize = calibrations.size(); i < calibrationsSize; i++) {
            Calibration calibration = calibrations.get(i);
            if (calibration.getFeatureIdentifier().equals(identifier())) {
                if (calibration.getFeatureVersion() >= currentVersion) {
                    // Derive lastRaw from existing contribution if not known
                    if (!hasRaw) {
                        double factor = co_efficient * weight;
                        if (factor != 0) {
                            lastRaw = value / factor;
                            hasRaw = true;
                        }
                    }
                    currentVersion = calibration.getFeatureVersion();
                    co_efficient = calibration.getCo_efficient();
                    weight = calibration.getWeight();
                    if (hasRaw) {
                        double newValue = lastRaw * co_efficient * weight;
                        boolean changed = newValue != value;
                        value = newValue;
                        return true;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Reset both coefficient and weight to one. If a raw value has already been observed, the
     * contribution is recomputed immediately.
     */
    @Override
    @NoPropagateFunction
    public boolean resetToOne() {
        // Derive lastRaw from existing contribution if not known and factor is non-zero
        if (!hasRaw) {
            double factor = co_efficient * weight;
            if (factor != 0) {
                lastRaw = value / factor;
                hasRaw = true;
            }
        }
        co_efficient = 1;
        weight = 1;
        if (hasRaw) {
            double newValue = lastRaw * co_efficient * weight;
            value = newValue;
            return true;
        }
        return true;
    }

    /**
     * Reset both coefficient and weight to zero, forcing the contribution to zero immediately.
     */
    @Override
    @NoPropagateFunction
    public boolean resetToZero() {
        // Derive lastRaw from existing contribution if not known and factor is non-zero
        if (!hasRaw) {
            double factor = co_efficient * weight;
            if (factor != 0) {
                lastRaw = value / factor;
                hasRaw = true;
            }
        }
        co_efficient = 0;
        weight = 0;
        // Force contribution to zero immediately
        value = 0;
        return true;
    }

    /**
     * Current contribution of the feature.
     */
    @Override
    public double value() {
        return value;
    }

    public double co_efficient() {
        return co_efficient;
    }

    public double weight() {
        return weight;
    }

    @Override
    public String identifier() {
        return identifier;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Helper for subclasses to update the feature contribution from a raw signal.
     * This method stores the raw value to support immediate recomputation upon calibration changes.
     *
     * @param raw last observed raw signal value
     * @return true if the computed contribution changed
     */
    protected final boolean updateFromRaw(double raw) {
        this.hasRaw = true;
        this.lastRaw = raw;
        double newValue = raw * co_efficient * weight;
        boolean changed = newValue != value;
        value = newValue;
        return changed;
    }
}
