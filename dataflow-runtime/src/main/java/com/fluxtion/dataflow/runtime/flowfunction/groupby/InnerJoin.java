/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.groupby;

import com.fluxtion.dataflow.runtime.flowfunction.Tuple;

public class InnerJoin extends AbstractJoin {

    @Override
    @SuppressWarnings("unckecked")
    public <K1, V1, K2 extends K1, V2> GroupBy<K1, Tuple<V1, V2>> join(
            GroupBy<K1, V1> leftGroupBy, GroupBy<K2, V2> rightGroupBY) {
        reset();
        if (leftGroupBy != null && rightGroupBY != null) {
            leftGroupBy.toMap().entrySet().forEach(e -> {
                V2 value2 = rightGroupBY.toMap().get(e.getKey());
                if (value2 != null) {
                    joinedGroup.toMap().put(
                            e.getKey(),
                            tupleObjectPool.checkOut().setFirst(e.getValue()).setSecond(value2));
                }
            });
        }
        return (GroupBy<K1, Tuple<V1, V2>>) (Object) joinedGroup;
    }

}
