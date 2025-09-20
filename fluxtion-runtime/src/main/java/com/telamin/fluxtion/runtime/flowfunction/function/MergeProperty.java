/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.function;

import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableBiConsumer;

/**
 * @param <T> The target type
 * @param <R> The consumer property type on the target and type of the incoming {@link FlowFunction}
 */
public class MergeProperty<T, R> {
    private final FlowFunction<R> trigger;
    private final SerializableBiConsumer<T, R> setValue;
    private final boolean triggering;
    private final boolean mandatory;

    public MergeProperty(FlowFunction<R> trigger, SerializableBiConsumer<T, R> setValue, boolean triggering, boolean mandatory) {
        this.trigger = trigger;
        this.setValue = setValue;
        this.triggering = triggering;
        this.mandatory = mandatory;
    }

    public FlowFunction<R> getTrigger() {
        return trigger;
    }

    public SerializableBiConsumer<T, R> getSetValue() {
        return setValue;
    }

    public boolean isTriggering() {
        return triggering;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void push(T target) {
        setValue.accept(target, trigger.get());
    }
}
