/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.groupby;

import com.fluxtion.dataflow.runtime.flowfunction.aggregate.AggregateFlowFunction;
import lombok.Value;

@Value
public class GroupByReduceFlowFunction {

    AggregateFlowFunction aggregateFunction;

    public <R> R reduceValues(GroupBy inputMap) {
        aggregateFunction.reset();
        inputMap.toMap().values().forEach(aggregateFunction::aggregate);
        return (R) aggregateFunction.get();
    }

    public Object reduceValues(Object inputMap) {
        return reduceValues((GroupBy) inputMap);
    }
}
