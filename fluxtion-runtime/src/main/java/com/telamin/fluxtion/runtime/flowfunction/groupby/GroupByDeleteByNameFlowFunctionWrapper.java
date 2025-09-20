/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.annotations.Initialise;
import com.telamin.fluxtion.runtime.annotations.OnParentUpdate;

import static com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;

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
