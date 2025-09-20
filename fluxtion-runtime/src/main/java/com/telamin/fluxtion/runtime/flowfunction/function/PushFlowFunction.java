/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.function;

import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.annotations.PushReference;
import com.telamin.fluxtion.runtime.flowfunction.DoubleFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.IntFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.LongFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableConsumer;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableDoubleConsumer;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableIntConsumer;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableLongConsumer;
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
