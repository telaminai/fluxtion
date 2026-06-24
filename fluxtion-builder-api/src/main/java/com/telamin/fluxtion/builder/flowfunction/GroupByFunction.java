/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.builder.flowfunction;

import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupByMapFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableBiFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;

public interface GroupByFunction {

    static <K, V, A, O, G extends GroupBy<K, V>> SerializableBiFunction<G, A, GroupBy<K, O>> mapValueByKey(
            SerializableBiFunction<V, A, O> mappingBiFunction,
            SerializableFunction<A, K> keyFunction) {
        GroupByMapFlowFunction invoker = new GroupByMapFlowFunction(keyFunction, mappingBiFunction, null);
        return invoker::mapKeyedValue;
    }

}
