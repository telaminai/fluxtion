/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.function;

import com.telamin.fluxtion.runtime.annotations.PushReference;
import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.flowfunction.FlowSupplier;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableQuadConsumer;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableTriConsumer;
import com.telamin.fluxtion.runtime.partition.MethodReferenceInfo;

public class TriPushFunction<T, A, B, C> extends MultiArgumentPushFunction<T> {

    @PushReference
    protected SerializableQuadConsumer<T, A, B, C> classPushMethod;
    @PushReference
    protected SerializableTriConsumer<A, B, C> instancePushMethod;
    protected FlowSupplier<A> source1;
    protected FlowSupplier<B> source2;
    protected FlowSupplier<C> source3;

    public TriPushFunction(@AssignToField("classPushMethod") SerializableQuadConsumer<T, A, B, C> classPushMethod,
                           @AssignToField("source1") FlowSupplier<A> source1,
                           @AssignToField("source2") FlowSupplier<B> source2,
                           @AssignToField("source3") FlowSupplier<C> source3) {
        super(classPushMethod, source1, source2, source3);
        this.classPushMethod = classPushMethod;
        this.source1 = source1;
        this.source2 = source2;
        this.source3 = source3;
    }

    public TriPushFunction(@AssignToField("instancePushMethod") SerializableTriConsumer<A, B, C> instancePushMethod,
                           @AssignToField("source1") FlowSupplier<A> source1,
                           @AssignToField("source2") FlowSupplier<B> source2,
                           @AssignToField("source3") FlowSupplier<C> source3) {
        super(instancePushMethod, source1, source2, source3);
        this.instancePushMethod = instancePushMethod;
        this.source1 = source1;
        this.source2 = source2;
        this.source3 = source3;
    }

    public TriPushFunction(@AssignToField("classPushMethod") SerializableQuadConsumer<T, A, B, C> classPushMethod,
                           @AssignToField("source1") FlowSupplier<A> source1,
                           @AssignToField("source2") FlowSupplier<B> source2,
                           @AssignToField("source3") FlowSupplier<C> source3,
                           T pushTarget,
                           MethodReferenceInfo methodReferenceInfo) {
        super(pushTarget, methodReferenceInfo, source1, source2, source3);
        this.classPushMethod = classPushMethod;
        this.source1 = source1;
        this.source2 = source2;
        this.source3 = source3;
    }

    public TriPushFunction(@AssignToField("instancePushMethod") SerializableTriConsumer<A, B, C> instancePushMethod,
                           @AssignToField("source1") FlowSupplier<A> source1,
                           @AssignToField("source2") FlowSupplier<B> source2,
                           @AssignToField("source3") FlowSupplier<C> source3,
                           T pushTarget,
                           MethodReferenceInfo methodReferenceInfo) {
        super(pushTarget, methodReferenceInfo, source1, source2, source3);
        this.instancePushMethod = instancePushMethod;
        this.source1 = source1;
        this.source2 = source2;
        this.source3 = source3;
    }

    public void triggerOperation() {
        A a = source1.get();
        B b = source2.get();
        C c = source3.get();
        auditLog.info("push", auditInfo)
                .info("a", a)
                .info("b", b)
                .info("c", c);
        if (instancePushMethod != null) {
            instancePushMethod.accept(a, b, c);
        } else {
            classPushMethod.accept(pushTarget, a, b, c);
        }
    }
}
