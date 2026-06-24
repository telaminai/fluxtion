/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;
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
