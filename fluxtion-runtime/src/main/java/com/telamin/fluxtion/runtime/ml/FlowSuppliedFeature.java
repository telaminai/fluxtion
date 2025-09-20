/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.ml;

import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.flowfunction.FlowSupplier;

/**
 * Inherit this class and add to {@link PredictiveLinearRegressionModel} to calculate a linear regression.
 * The subclass implements {@link #extractFeatureValue()}, all the event triggering and co_efficient adjustments are
 * implemented in this class.
 *
 * @param <T>
 */
public abstract class FlowSuppliedFeature<T> extends AbstractFeature implements CalibrationProcessor {
    protected final FlowSupplier<T> dataFlowSupplier;

    public FlowSuppliedFeature(FlowSupplier<T> dataFlowSupplier) {
        this.dataFlowSupplier = dataFlowSupplier;
    }

    public FlowSuppliedFeature(
            @AssignToField("name") String name,
            @AssignToField("identifier") String identifier,
            FlowSupplier<T> dataFlowSupplier) {
        super(name, identifier);
        this.dataFlowSupplier = dataFlowSupplier;
    }

    @OnTrigger
    public boolean calculateFeature() {
        double newValue = extractFeatureValue() * co_efficient * weight;
        boolean changed = newValue != value;
        value = newValue;
        return changed;
    }

    /**
     * Implement this method to extract the value of this feature as a double
     *
     * @return the feature value
     */
    public abstract double extractFeatureValue();

    protected T data() {
        return dataFlowSupplier.get();
    }
}
