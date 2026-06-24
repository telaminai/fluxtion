/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.ml;

import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.flowfunction.FlowSupplier;
import com.telamin.fluxtion.runtime.partition.LambdaReflection;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableToDoubleFunction;

/**
 * uses a lambda {@link SerializableToDoubleFunction} to extract a property from an incoming FlowSupplier and use that as
 * the value for a feature.
 *
 * @param <T>
 */
public class PropertyToFeature<T> extends FlowSuppliedFeature<T> {
    private final SerializableToDoubleFunction<T> propertyExtractor;

    public static <T> PropertyToFeature<T> build(
            String name,
            FlowSupplier<T> dataFlowSupplier,
            SerializableToDoubleFunction<T> propertyExtractor) {
        return new PropertyToFeature<>(name, dataFlowSupplier, propertyExtractor);
    }

    public static <T, S> MapPropertyToFeature<T, S> build(
            String name,
            FlowSupplier<T> dataFlowSupplier,
            LambdaReflection.SerializableFunction<T, S> propertyExtractor,
            SerializableToDoubleFunction<S> propertyMapper) {
        return new MapPropertyToFeature<>(name, dataFlowSupplier, propertyExtractor, propertyMapper);
    }

    public PropertyToFeature(
            @AssignToField("name") String name,
            @AssignToField("dataFlowSupplier") FlowSupplier<T> dataFlowSupplier,
            @AssignToField("propertyExtractor") SerializableToDoubleFunction<T> propertyExtractor) {
        super(name, name, dataFlowSupplier);
        this.propertyExtractor = propertyExtractor;
    }

    @Override
    public double extractFeatureValue() {
        return propertyExtractor.applyAsDouble(data());
    }
}

