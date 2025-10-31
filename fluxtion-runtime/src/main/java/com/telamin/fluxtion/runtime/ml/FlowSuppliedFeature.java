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
 * Base for features that read data from a {@link FlowSupplier} and contribute to a
 * {@link PredictiveLinearRegressionModel}. The subclass implements {@link #extractFeatureValue()} to return
 * the raw signal, while this base class handles triggering and applies the current
 * {@code co_efficient * weight} via {@link #updateFromRaw(double)}.
 *
 * @param <T> input type of the upstream flow
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
        return updateFromRaw(extractFeatureValue());
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
