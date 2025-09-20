/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.function;

import com.fluxtion.dataflow.runtime.annotations.PushReference;
import com.fluxtion.dataflow.runtime.annotations.builder.AssignToField;
import com.fluxtion.dataflow.runtime.flowfunction.FlowSupplier;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableQuinConsumer;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableSextConsumer;

public class QuinPushFunction<T, A, B, C, D, E> extends MultiArgumentPushFunction<T> {

    @PushReference
    protected SerializableSextConsumer<T, A, B, C, D, E> classPushMethod;
    @PushReference
    protected SerializableQuinConsumer<A, B, C, D, E> instancePushMethod;
    protected FlowSupplier<A> source1;
    protected FlowSupplier<B> source2;
    protected FlowSupplier<C> source3;
    protected FlowSupplier<D> source4;
    protected FlowSupplier<E> source5;

    public QuinPushFunction(@AssignToField("classPushMethod") SerializableSextConsumer<T, A, B, C, D, E> classPushMethod,
                            @AssignToField("source1") FlowSupplier<A> source1,
                            @AssignToField("source2") FlowSupplier<B> source2,
                            @AssignToField("source3") FlowSupplier<C> source3,
                            @AssignToField("source4") FlowSupplier<D> source4,
                            @AssignToField("source5") FlowSupplier<E> source5) {
        super(classPushMethod, source1, source2, source3, source4, source5);
        this.classPushMethod = classPushMethod;
        this.source1 = source1;
        this.source2 = source2;
        this.source3 = source3;
        this.source4 = source4;
        this.source5 = source5;
    }

    public QuinPushFunction(@AssignToField("instancePushMethod") SerializableQuinConsumer<A, B, C, D, E> instancePushMethod,
                            @AssignToField("source1") FlowSupplier<A> source1,
                            @AssignToField("source2") FlowSupplier<B> source2,
                            @AssignToField("source3") FlowSupplier<C> source3,
                            @AssignToField("source4") FlowSupplier<D> source4,
                            @AssignToField("source5") FlowSupplier<E> source5) {
        super(instancePushMethod, source1, source2, source3, source4, source5);
        this.instancePushMethod = instancePushMethod;
        this.source1 = source1;
        this.source2 = source2;
        this.source3 = source3;
        this.source4 = source4;
        this.source5 = source5;
    }

    public void triggerOperation() {
        A a = source1.get();
        B b = source2.get();
        C c = source3.get();
        D d = source4.get();
        E e = source5.get();
        auditLog.info("push", auditInfo)
                .info("a", a)
                .info("b", b)
                .info("c", c)
                .info("d", d)
                .info("e", e);
        if (instancePushMethod != null) {
            instancePushMethod.accept(a, b, c, d, e);
        } else {
            classPushMethod.accept(pushTarget, a, b, c, d, e);
        }
    }
}
