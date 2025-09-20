/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.ml;

import com.fluxtion.dataflow.runtime.annotations.ExportService;
import com.fluxtion.dataflow.runtime.annotations.feature.Experimental;
import com.fluxtion.dataflow.runtime.flowfunction.FlowSupplier;
import com.fluxtion.dataflow.runtime.node.NamedNode;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection;

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
