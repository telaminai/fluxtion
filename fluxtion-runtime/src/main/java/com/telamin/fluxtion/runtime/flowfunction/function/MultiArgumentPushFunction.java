/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.function;

import com.telamin.fluxtion.runtime.annotations.NoTriggerReference;
import com.telamin.fluxtion.runtime.annotations.PushReference;
import com.telamin.fluxtion.runtime.annotations.builder.FluxtionIgnore;
import com.telamin.fluxtion.runtime.context.buildtime.GeneratorNodeCollection;
import com.telamin.fluxtion.runtime.flowfunction.FlowSupplier;
import com.telamin.fluxtion.runtime.flowfunction.Stateful;
import com.telamin.fluxtion.runtime.partition.LambdaReflection;
import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public abstract class MultiArgumentPushFunction<T> extends BaseFlowNode<T> {

    @PushReference
    protected T pushTarget;
    @Getter
    @FluxtionIgnore
    private final boolean statefulFunction;
    @NoTriggerReference
    protected transient Stateful<?> resetFunction;
    protected transient final String auditInfo;

    @SuppressWarnings("all")
    MultiArgumentPushFunction(LambdaReflection.MethodReferenceReflection methodReference, FlowSupplier<?>... flowSuppliers) {
        Objects.requireNonNull(methodReference, "push methodReference cannot be null");
        Objects.requireNonNull(flowSuppliers, "flowSuppliers cannot be null");
        if (methodReference.isDefaultConstructor()) {
            throw new IllegalArgumentException("push methodReference must not be defaultConstructor");
        }
        if (flowSuppliers.length == 0) {
            throw new IllegalArgumentException("flowSuppliers cannot be empty");
        }

        if (methodReference.captured().length == 0) {
            try {
                pushTarget = (T) methodReference.getContainingClass().getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException("no default constructor found for class:"
                        + methodReference.getContainingClass()
                        + " either add default constructor or pass in a node instance");
            }
        } else {
            pushTarget = (T) GeneratorNodeCollection.service().addOrReuse(methodReference.captured()[0]);
        }

        statefulFunction = Stateful.class.isAssignableFrom(pushTarget.getClass());
        if (statefulFunction) {
            resetFunction = (Stateful) methodReference.captured()[0];
        }

        for (FlowSupplier<?> flowSupplier : flowSuppliers) {
            getInputs().add(flowSupplier);
        }

        auditInfo = methodReference.method().getDeclaringClass().getSimpleName() + "->" + methodReference.method().getName();
    }


    @Override
    protected void resetOperation() {
        resetFunction.reset();
    }

    @Override
    public T get() {
        return pushTarget;
    }

}
