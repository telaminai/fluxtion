/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.flowfunction.Tuple;

public class LeftJoin extends AbstractJoin {

    @Override
    @SuppressWarnings("unckecked")
    public <K1, V1, K2 extends K1, V2> GroupBy<K1, Tuple<V1, V2>> join(
            GroupBy<K1, V1> leftGroupBy, GroupBy<K2, V2> rightGroupBY) {
        reset();
        if (leftGroupBy != null) {
            leftGroupBy.toMap().entrySet().forEach(left -> {
                V2 right = rightGroupBY == null ? null : rightGroupBY.toMap().get(left.getKey());
//                joinedGroup.toMap().put(left.getKey(), Tuple.build(left.getValue(), right));
                joinedGroup.toMap().put(
                        left.getKey(),
                        tupleObjectPool.checkOut().setFirst(left.getValue()).setSecond(right));
            });
        }
        return (GroupBy<K1, Tuple<V1, V2>>) (Object) joinedGroup;
    }

}
