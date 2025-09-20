/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.flowfunction;

import com.fluxtion.dataflow.runtime.flowfunction.groupby.GroupBy;
import com.fluxtion.dataflow.runtime.flowfunction.groupby.GroupByMapFlowFunction;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableBiFunction;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableFunction;

public interface GroupByFunction {

    static <K, V, A, O, G extends GroupBy<K, V>> SerializableBiFunction<G, A, GroupBy<K, O>> mapValueByKey(
            SerializableBiFunction<V, A, O> mappingBiFunction,
            SerializableFunction<A, K> keyFunction) {
        GroupByMapFlowFunction invoker = new GroupByMapFlowFunction(keyFunction, mappingBiFunction, null);
        return invoker::mapKeyedValue;
    }

}
