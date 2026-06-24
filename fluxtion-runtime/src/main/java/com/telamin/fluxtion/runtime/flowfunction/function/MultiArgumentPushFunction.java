/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.function;

import com.telamin.fluxtion.runtime.annotations.NoTriggerReference;
import com.telamin.fluxtion.runtime.annotations.PushReference;
import com.telamin.fluxtion.runtime.annotations.builder.FluxtionIgnore;
import com.telamin.fluxtion.runtime.context.buildtime.GeneratorNodeCollection;
import com.telamin.fluxtion.runtime.flowfunction.FlowSupplier;
import com.telamin.fluxtion.runtime.flowfunction.Stateful;
import com.telamin.fluxtion.runtime.partition.LambdaReflection;
import com.telamin.fluxtion.runtime.partition.MethodReferenceInfo;
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

    /**
     * Closed-world constructor — the {@code pushTarget} and all method-reference
     * metadata ({@code auditInfo}, stateful/reset) are resolved by the generator at
     * build time, so this path performs no {@code SerializedLambda} introspection on
     * the push method reference and instantiates no class reflectively. Compatible
     * with native-image / TeaVM-WASM. The push method reference is still passed to
     * the subclass purely so it can be <em>invoked</em> (never introspected).
     */
    @SuppressWarnings("unchecked")
    MultiArgumentPushFunction(T pushTarget, MethodReferenceInfo methodReferenceInfo, FlowSupplier<?>... flowSuppliers) {
        Objects.requireNonNull(pushTarget, "pushTarget cannot be null");
        Objects.requireNonNull(flowSuppliers, "flowSuppliers cannot be null");
        if (flowSuppliers.length == 0) {
            throw new IllegalArgumentException("flowSuppliers cannot be empty");
        }
        this.pushTarget = pushTarget;
        this.statefulFunction = methodReferenceInfo.isStateful();
        if (statefulFunction) {
            this.resetFunction = (Stateful<?>) methodReferenceInfo.getResetReference();
        }
        for (FlowSupplier<?> flowSupplier : flowSuppliers) {
            getInputs().add(flowSupplier);
        }
        this.auditInfo = methodReferenceInfo.getAuditName();
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
