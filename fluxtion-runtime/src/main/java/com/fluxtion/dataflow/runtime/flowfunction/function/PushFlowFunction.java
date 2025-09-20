/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.function;

import com.fluxtion.dataflow.runtime.annotations.OnTrigger;
import com.fluxtion.dataflow.runtime.annotations.PushReference;
import com.fluxtion.dataflow.runtime.flowfunction.DoubleFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.FlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.IntFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.LongFlowFunction;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableConsumer;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableDoubleConsumer;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableIntConsumer;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableLongConsumer;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString
public class PushFlowFunction<T, S extends FlowFunction<T>> extends AbstractFlowFunction<T, T, S> {

    @PushReference
    private final SerializableConsumer<T> eventStreamConsumer;
    private transient final String auditInfo;

    public PushFlowFunction(S inputEventStream, SerializableConsumer<T> eventStreamConsumer) {
        super(inputEventStream, null);
        this.eventStreamConsumer = eventStreamConsumer;
        auditInfo = eventStreamConsumer.method().getDeclaringClass().getSimpleName() + "->" + eventStreamConsumer.method().getName();
    }

    @OnTrigger
    public boolean push() {
        auditLog.info("pushTarget", auditInfo);
        if (executeUpdate()) {
            eventStreamConsumer.accept(get());
        }
        return fireEventUpdateNotification();
    }

    @Override
    public T get() {
        return getInputEventStream().get();
    }

    @EqualsAndHashCode(callSuper = true)
    @ToString
    public static class IntPushFlowFunction extends AbstractFlowFunction<Integer, Integer, IntFlowFunction> implements IntFlowFunction {

        @PushReference
        private final SerializableIntConsumer intConsumer;
        private transient final String auditInfo;

        public IntPushFlowFunction(IntFlowFunction inputEventStream, SerializableIntConsumer intConsumer) {
            super(inputEventStream, null);
            this.intConsumer = intConsumer;
            auditInfo = intConsumer.method().getDeclaringClass().getSimpleName() + "->" + intConsumer.method().getName();
        }

        @OnTrigger
        public boolean push() {
            auditLog.info("pushTarget", auditInfo);
            if (executeUpdate()) {
                intConsumer.accept(getAsInt());
            }
            return fireEventUpdateNotification();
        }

        @Override
        public int getAsInt() {
            return getInputEventStream().getAsInt();
        }

        @Override
        public Integer get() {
            return getAsInt();
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @ToString
    public static class DoublePushFlowFunction extends AbstractFlowFunction<Double, Double, DoubleFlowFunction> implements DoubleFlowFunction {

        @PushReference
        private final SerializableDoubleConsumer intConsumer;
        private transient final String auditInfo;

        public DoublePushFlowFunction(DoubleFlowFunction inputEventStream, SerializableDoubleConsumer intConsumer) {
            super(inputEventStream, null);
            this.intConsumer = intConsumer;
            auditInfo = intConsumer.method().getDeclaringClass().getSimpleName() + "->" + intConsumer.method().getName();
        }

        @OnTrigger
        public boolean push() {
            auditLog.info("pushTarget", auditInfo);
            if (executeUpdate()) {
                intConsumer.accept(getAsDouble());
            }
            return fireEventUpdateNotification();
        }

        @Override
        public double getAsDouble() {
            return getInputEventStream().getAsDouble();
        }

        @Override
        public Double get() {
            return getAsDouble();
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @ToString
    public static class LongPushFlowFunction extends AbstractFlowFunction<Long, Long, LongFlowFunction> implements LongFlowFunction {

        @PushReference
        private final SerializableLongConsumer intConsumer;
        private transient final String auditInfo;

        public LongPushFlowFunction(LongFlowFunction inputEventStream, SerializableLongConsumer intConsumer) {
            super(inputEventStream, null);
            this.intConsumer = intConsumer;
            auditInfo = intConsumer.method().getDeclaringClass().getSimpleName() + "->" + intConsumer.method().getName();
        }

        @OnTrigger
        public boolean push() {
            auditLog.info("pushTarget", auditInfo);
            if (executeUpdate()) {
                intConsumer.accept(getAsLong());
            }
            return fireEventUpdateNotification();
        }

        @Override
        public long getAsLong() {
            return getInputEventStream().getAsLong();
        }

        @Override
        public Long get() {
            return getAsLong();
        }
    }
}
