/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.function;

import com.fluxtion.dataflow.runtime.annotations.NoTriggerReference;
import com.fluxtion.dataflow.runtime.annotations.OnTrigger;
import com.fluxtion.dataflow.runtime.flowfunction.DoubleFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.FlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.IntFlowFunction;
import com.fluxtion.dataflow.runtime.flowfunction.LongFlowFunction;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableConsumer;
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
