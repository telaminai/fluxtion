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
import com.telamin.fluxtion.runtime.annotations.feature.Experimental;
import com.telamin.fluxtion.runtime.flowfunction.FlowSupplier;
import com.telamin.fluxtion.runtime.node.NamedNode;
import com.telamin.fluxtion.runtime.partition.LambdaReflection;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.List;

@Experimental
public interface Feature extends NamedNode, @ExportService CalibrationProcessor {

    default String identifier() {
        return getClass().getSimpleName();
    }

    @Override
    default String getName() {
        return Introspector.decapitalize(identifier());
    }

    double value();

    @SafeVarargs
    static <T> List<Feature> include(FlowSupplier<T> inputDataFlow, LambdaReflection.SerializableToDoubleFunction<T>... featureExtractors) {
        List<Feature> featureList = new ArrayList<>(featureExtractors.length);
        for (LambdaReflection.SerializableToDoubleFunction<T> featureExtractor : featureExtractors) {
            featureList.add(
                    PropertyToFeature.build(featureExtractor.method().getName(), inputDataFlow, featureExtractor)
            );
        }
        return featureList;
    }

}
