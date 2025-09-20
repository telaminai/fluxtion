/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.ml;

import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.flowfunction.FlowSupplier;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableToDoubleFunction;

/**
 * uses a lambda {@link SerializableToDoubleFunction} to extract a property from an incoming FlowSupplier
 * then applies a mapping function to supply a double as the value for the feature.
 *
 * @param <T>
 */
public class MapPropertyToFeature<T, S> extends FlowSuppliedFeature<T> {
    private final SerializableFunction<T, S> propertyExtractor;
    private final SerializableToDoubleFunction<S> propertyMapper;

    public MapPropertyToFeature(
            @AssignToField("name") String name,
            @AssignToField("dataFlowSupplier") FlowSupplier<T> dataFlowSupplier,
            @AssignToField("propertyExtractor") SerializableFunction<T, S> propertyExtractor,
            @AssignToField("propertyMapper") SerializableToDoubleFunction<S> propertyMapper
    ) {
        super(name, name, dataFlowSupplier);
        this.propertyExtractor = propertyExtractor;
        this.propertyMapper = propertyMapper;
    }

    @Override
    public double extractFeatureValue() {
        return propertyMapper.applyAsDouble(propertyExtractor.apply(data()));
    }
}

