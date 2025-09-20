/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.groupby;

import com.fluxtion.dataflow.runtime.annotations.Initialise;
import com.fluxtion.dataflow.runtime.annotations.OnParentUpdate;

import static com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableFunction;

public class GroupByDeleteByNameFlowFunctionWrapper {

    private final SerializableFunction<Object, Boolean> deletePredicateFunction;
    private Object trigger;
    private boolean applyPredicate;

    @SuppressWarnings({"unchecked"})
    public <T> GroupByDeleteByNameFlowFunctionWrapper(SerializableFunction<T, Boolean> deletePredicateFunction) {
        this.deletePredicateFunction = (SerializableFunction<Object, Boolean>) deletePredicateFunction;
        this.trigger = null;
    }

    @SuppressWarnings({"unchecked"})
    public <T> GroupByDeleteByNameFlowFunctionWrapper(SerializableFunction<T, Boolean> deletePredicateFunction, Object trigger) {
        this.deletePredicateFunction = (SerializableFunction<Object, Boolean>) deletePredicateFunction;
        this.trigger = trigger;
    }

    @Initialise
    public void init() {
        applyPredicate = false;
    }

    @OnParentUpdate("trigger")
    public void predicateUpdated(Object trigger) {
        applyPredicate = true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public GroupBy deleteByKey(GroupBy groupBy, Object keysToDelete) {
        if (applyPredicate) {
            groupBy.toMap().values().removeIf(deletePredicateFunction::apply);
        }
        applyPredicate = false;
        return groupBy;
    }
}
