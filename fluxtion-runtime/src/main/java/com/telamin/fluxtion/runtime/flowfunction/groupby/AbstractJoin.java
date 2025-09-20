/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.flowfunction.Stateful;
import com.telamin.fluxtion.runtime.flowfunction.Tuple;
import com.telamin.fluxtion.runtime.util.ObjectPool;

public abstract class AbstractJoin implements Stateful<GroupBy> {
    //GroupBy<K1, Tuple<V1, V2>> joinedGroup = new GroupByHashMap<>();
    protected final transient GroupByHashMap<Object, MutableTuple<Object, Object>> joinedGroup = new GroupByHashMap<>();
    protected final transient ObjectPool<MutableTuple> tupleObjectPool = new ObjectPool<>(MutableTuple::new);

    @SuppressWarnings("unckecked")
    public abstract <K1, V1, K2 extends K1, V2> GroupBy<K1, Tuple<V1, V2>> join(
            GroupBy<K1, V1> leftGroupBy, GroupBy<K2, V2> rightGroupBY);

    //hack for incomplete generics in generated code
    public GroupBy join(Object leftGroupBy, Object rightGroupBY) {
        return this.join((GroupBy) leftGroupBy, (GroupBy) rightGroupBY);
    }

    @Override
    public GroupBy reset() {
        joinedGroup.values().forEach(t -> t.returnToPool(tupleObjectPool));
        return joinedGroup.reset();
    }
}
