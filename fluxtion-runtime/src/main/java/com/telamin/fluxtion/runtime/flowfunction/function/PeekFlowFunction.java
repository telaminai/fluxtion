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
import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.flowfunction.DoubleFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.IntFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.LongFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableConsumer;
import lombok.ToString;

public class PeekFlowFunction<T, S extends FlowFunction<T>> extends AbstractFlowFunction<T, T, S> {

    @NoTriggerReference
    final SerializableConsumer<? super T> eventStreamConsumer;
    transient final String auditInfo;

    public PeekFlowFunction(S inputEventStream, SerializableConsumer<? super T> eventStreamConsumer) {
        super(inputEventStream, eventStreamConsumer);
        this.eventStreamConsumer = eventStreamConsumer;
        auditInfo = eventStreamConsumer.method().getDeclaringClass().getSimpleName()
                + "->" + eventStreamConsumer.method().getName();
    }

    @OnTrigger
    public void peek() {
        auditLog.info("peekConsumer", auditInfo);
        eventStreamConsumer.accept(get());
    }

    @Override
    public T get() {
        return getInputEventStream().get();
    }

    @ToString
    public static class IntPeekFlowFunction extends PeekFlowFunction<Integer, IntFlowFunction> implements IntFlowFunction {

        public IntPeekFlowFunction(IntFlowFunction inputEventStream, SerializableConsumer<? super Integer> eventStreamConsumer) {
            super(inputEventStream, eventStreamConsumer);
        }

        @Override
        public int getAsInt() {
            return getInputEventStream().getAsInt();
        }
    }


    @ToString
    public static class DoublePeekFlowFunction extends PeekFlowFunction<Double, DoubleFlowFunction> implements DoubleFlowFunction {

        public DoublePeekFlowFunction(DoubleFlowFunction inputEventStream, SerializableConsumer<? super Double> eventStreamConsumer) {
            super(inputEventStream, eventStreamConsumer);
        }

        @Override
        public double getAsDouble() {
            return getInputEventStream().getAsDouble();
        }
    }


    @ToString
    public static class LongPeekFlowFunction extends PeekFlowFunction<Long, LongFlowFunction> implements LongFlowFunction {

        public LongPeekFlowFunction(LongFlowFunction inputEventStream, SerializableConsumer<? super Long> eventStreamConsumer) {
            super(inputEventStream, eventStreamConsumer);
        }

        @Override
        public long getAsLong() {
            return getInputEventStream().getAsLong();
        }
    }

}
