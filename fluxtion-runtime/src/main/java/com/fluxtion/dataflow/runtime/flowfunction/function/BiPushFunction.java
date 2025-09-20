/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.function;

import com.fluxtion.dataflow.runtime.annotations.PushReference;
import com.fluxtion.dataflow.runtime.annotations.builder.AssignToField;
import com.fluxtion.dataflow.runtime.flowfunction.FlowSupplier;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableBiConsumer;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableTriConsumer;

public class BiPushFunction<T, A, B> extends MultiArgumentPushFunction<T> {

    @PushReference
    protected SerializableTriConsumer<T, A, B> classPushMethod;
    @PushReference
    protected SerializableBiConsumer<A, B> instancePushMethod;
    protected FlowSupplier<A> source1;
    protected FlowSupplier<B> source2;

    public BiPushFunction(
            @AssignToField("classPushMethod") SerializableTriConsumer<T, A, B> classPushMethod,
            @AssignToField("source1") FlowSupplier<A> source1,
            @AssignToField("source2") FlowSupplier<B> source2
    ) {
        super(classPushMethod, source1, source2);
        this.classPushMethod = classPushMethod;
        this.source1 = source1;
        this.source2 = source2;
    }

    public BiPushFunction(
            @AssignToField("instancePushMethod") SerializableBiConsumer<A, B> instancePushMethod,
            @AssignToField("source1") FlowSupplier<A> source1,
            @AssignToField("source2") FlowSupplier<B> source2
    ) {
        super(instancePushMethod, source1, source2);
        this.instancePushMethod = instancePushMethod;
        this.source1 = source1;
        this.source2 = source2;
    }

    public void triggerOperation() {
        A a = source1.get();
        B b = source2.get();
        auditLog.info("push", auditInfo)
                .info("a", a)
                .info("b", b);
        if (instancePushMethod != null) {
            instancePushMethod.accept(a, b);
        } else {
            classPushMethod.accept(pushTarget, a, b);
        }
    }
}
