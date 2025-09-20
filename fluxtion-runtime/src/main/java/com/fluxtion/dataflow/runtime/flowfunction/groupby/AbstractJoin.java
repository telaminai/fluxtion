/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.groupby;

import com.fluxtion.dataflow.runtime.flowfunction.Stateful;
import com.fluxtion.dataflow.runtime.flowfunction.Tuple;
import com.fluxtion.dataflow.runtime.util.ObjectPool;

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
