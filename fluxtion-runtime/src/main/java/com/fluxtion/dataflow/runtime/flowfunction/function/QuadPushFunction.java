/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.function;

import com.fluxtion.dataflow.runtime.annotations.PushReference;
import com.fluxtion.dataflow.runtime.annotations.builder.AssignToField;
import com.fluxtion.dataflow.runtime.flowfunction.FlowSupplier;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableQuadConsumer;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableQuinConsumer;

public class QuadPushFunction<T, A, B, C, D> extends MultiArgumentPushFunction<T> {

    @PushReference
    protected SerializableQuinConsumer<T, A, B, C, D> classPushMethod;
    @PushReference
    protected SerializableQuadConsumer<A, B, C, D> instancePushMethod;
    protected FlowSupplier<A> source1;
    protected FlowSupplier<B> source2;
    protected FlowSupplier<C> source3;
    protected FlowSupplier<D> source4;

    public QuadPushFunction(@AssignToField("classPushMethod") SerializableQuinConsumer<T, A, B, C, D> classPushMethod,
                            @AssignToField("source1") FlowSupplier<A> source1,
                            @AssignToField("source2") FlowSupplier<B> source2,
                            @AssignToField("source3") FlowSupplier<C> source3,
                            @AssignToField("source4") FlowSupplier<D> source4) {
        super(classPushMethod, source1, source2, source3, source4);
        this.classPushMethod = classPushMethod;
        this.source1 = source1;
        this.source2 = source2;
        this.source3 = source3;
        this.source4 = source4;
    }

    public QuadPushFunction(@AssignToField("instancePushMethod") SerializableQuadConsumer<A, B, C, D> instancePushMethod,
                            @AssignToField("source1") FlowSupplier<A> source1,
                            @AssignToField("source2") FlowSupplier<B> source2,
                            @AssignToField("source3") FlowSupplier<C> source3,
                            @AssignToField("source4") FlowSupplier<D> source4) {
        super(instancePushMethod, source1, source2, source3, source4);
        this.instancePushMethod = instancePushMethod;
        this.source1 = source1;
        this.source2 = source2;
        this.source3 = source3;
        this.source4 = source4;
    }

    public void triggerOperation() {
        A a = source1.get();
        B b = source2.get();
        C c = source3.get();
        D d = source4.get();
        auditLog.info("push", auditInfo)
                .info("a", a)
                .info("b", b)
                .info("c", c)
                .info("d", d);
        if (instancePushMethod != null) {
            instancePushMethod.accept(a, b, c, d);
        } else {
            classPushMethod.accept(pushTarget, a, b, c, d);
        }
    }
}
