/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.function;

import com.telamin.fluxtion.runtime.annotations.PushReference;
import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.flowfunction.FlowSupplier;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSeptConsumer;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSextConsumer;

public class SextPushFunction<T, A, B, C, D, E, F> extends MultiArgumentPushFunction<T> {

    @PushReference
    protected SerializableSeptConsumer<T, A, B, C, D, E, F> classPushMethod;
    @PushReference
    protected SerializableSextConsumer<A, B, C, D, E, F> instancePushMethod;
    protected FlowSupplier<A> source1;
    protected FlowSupplier<B> source2;
    protected FlowSupplier<C> source3;
    protected FlowSupplier<D> source4;
    protected FlowSupplier<E> source5;
    protected FlowSupplier<F> source6;

    public SextPushFunction(@AssignToField("classPushMethod") SerializableSeptConsumer<T, A, B, C, D, E, F> classPushMethod,
                            @AssignToField("source1") FlowSupplier<A> source1,
                            @AssignToField("source2") FlowSupplier<B> source2,
                            @AssignToField("source3") FlowSupplier<C> source3,
                            @AssignToField("source4") FlowSupplier<D> source4,
                            @AssignToField("source5") FlowSupplier<E> source5,
                            @AssignToField("source6") FlowSupplier<F> source6) {
        super(classPushMethod, source1, source2, source3, source4, source5, source6);
        this.classPushMethod = classPushMethod;
        this.source1 = source1;
        this.source2 = source2;
        this.source3 = source3;
        this.source4 = source4;
        this.source5 = source5;
        this.source6 = source6;
    }

    public SextPushFunction(@AssignToField("instancePushMethod") SerializableSextConsumer<A, B, C, D, E, F> instancePushMethod,
                            @AssignToField("source1") FlowSupplier<A> source1,
                            @AssignToField("source2") FlowSupplier<B> source2,
                            @AssignToField("source3") FlowSupplier<C> source3,
                            @AssignToField("source4") FlowSupplier<D> source4,
                            @AssignToField("source5") FlowSupplier<E> source5,
                            @AssignToField("source6") FlowSupplier<F> source6) {
        super(instancePushMethod, source1, source2, source3, source4, source5, source6);
        this.instancePushMethod = instancePushMethod;
        this.source1 = source1;
        this.source2 = source2;
        this.source3 = source3;
        this.source4 = source4;
        this.source5 = source5;
        this.source6 = source6;
    }

    public void triggerOperation() {
        A a = source1.get();
        B b = source2.get();
        C c = source3.get();
        D d = source4.get();
        E e = source5.get();
        F f = source6.get();
        auditLog.info("push", auditInfo)
                .info("a", a)
                .info("b", b)
                .info("c", c)
                .info("d", d)
                .info("e", e)
                .info("f", f);
        if (instancePushMethod != null) {
            instancePushMethod.accept(a, b, c, d, e, f);
        } else {
            classPushMethod.accept(pushTarget, a, b, c, d, e, f);
        }
    }
}
