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

    private final transient String name;
    private final transient String identifier;
    protected double co_efficient;
    protected double weight;
    protected double value;

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

    @Initialise
    public void init() {
        co_efficient = 0;
        weight = 0;
        value = 0;
    }

    @Override
    @NoPropagateFunction
    public boolean setCalibration(List<Calibration> calibrations) {
        for (int i = 0, calibrationsSize = calibrations.size(); i < calibrationsSize; i++) {
            Calibration calibration = calibrations.get(i);
            if (calibration.getFeatureIdentifier().equals(identifier())) {
                co_efficient = calibration.getCo_efficient();
                weight = calibration.getWeight();
                return true;
            }
        }
        return false;
    }

    @Override
    @NoPropagateFunction
    public boolean resetToOne() {
        co_efficient = 1;
        weight = 1;
        return false;
    }

    @Override
    @NoPropagateFunction
    public boolean resetToZero() {
        co_efficient = 0;
        weight = 0;
        return false;
    }

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
}
