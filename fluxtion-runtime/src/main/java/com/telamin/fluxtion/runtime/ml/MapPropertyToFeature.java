/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
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

