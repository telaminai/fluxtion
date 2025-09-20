/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.groupby;

import com.fluxtion.dataflow.runtime.flowfunction.Tuple;

public class RightJoin extends AbstractJoin {

    @Override
    @SuppressWarnings("unckecked")
    public <K1, V1, K2 extends K1, V2> GroupBy<K1, Tuple<V1, V2>> join(
            GroupBy<K1, V1> leftGroupBy, GroupBy<K2, V2> rightGroupBY) {
        reset();
        if (rightGroupBY != null) {
            rightGroupBY.toMap().entrySet().forEach(right -> {
                V1 left = leftGroupBy == null ? null : leftGroupBy.toMap().get(right.getKey());
                joinedGroup.toMap().put(
                        right.getKey(),
                        tupleObjectPool.checkOut().setFirst(left).setSecond(right.getValue()));
            });
        }
        return (GroupBy<K1, Tuple<V1, V2>>) (Object) joinedGroup;
    }

}
