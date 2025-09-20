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
import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.flowfunction.*;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.MethodReferenceReflection;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableBiDoubleFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableBiIntFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableBiLongFunction;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.lang.reflect.Method;

import static com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableBiFunction;

/**
 * @param <R> Type of input stream for first argument
 * @param <Q> Type of input stream for second argument
 * @param <T> Output type of this stream
 * @param <S> The type of {@link FlowFunction} that wraps R
 * @param <U> The type of {@link FlowFunction} that wraps Q
 */
public abstract class BinaryMapFlowFunction<R, Q, T, S extends FlowFunction<R>, U extends FlowFunction<Q>>
        extends AbstractFlowFunction.AbstractBinaryEventStream<R, Q, T, S, U> {

    protected transient String auditInfo;
    protected transient T result;
    @Getter
    @Setter
    @Accessors(fluent = true)
    protected T defaultValue;

    public BinaryMapFlowFunction(
            S inputEventStream,
            U inputEventStream_2,
            MethodReferenceReflection methodReferenceReflection) {
        super(inputEventStream, inputEventStream_2, methodReferenceReflection);
        Method method = methodReferenceReflection.method();
        auditInfo = method.getDeclaringClass().getSimpleName() + "->" + method.getName();
    }

    @OnTrigger
    public final boolean map() {
        auditLog.info("mapFunction", auditInfo);
        if (executeUpdate()) {
            auditLog.info("invokeMapFunction", true);
            mapOperation();
        } else if (reset()) {
            auditLog.info("invokeMapFunction", false);
            auditLog.info("reset", true);
//            resetOperation();
        } else {
            auditLog.info("invokeMapFunction", false);
        }
        return fireEventUpdateNotification();
    }

    @Override
    public boolean hasDefaultValue() {
        return defaultValue != null | DefaultValueSupplier.class.isAssignableFrom(getStreamFunction().method().getDeclaringClass());
    }

    @Override
    public T get() {
        return result == null ? defaultValue : result;
    }

    abstract protected void mapOperation();

    protected void resetOperation() {
        inputStreamTriggered_2 = false;
        if (resetFunction != null)
            result = resetFunction.reset();
//        super.resetOperation();
//        System.out.println("Call to binary function reset - not implemented");
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    public static class BinaryMapToRefFlowFunction<R, Q, T, S extends FlowFunction<R>, U extends FlowFunction<Q>>
            extends BinaryMapFlowFunction<R, Q, T, S, U> {

        private final SerializableBiFunction<R, Q, T> mapFunction;

        public BinaryMapToRefFlowFunction(
                @AssignToField("inputEventStream") S inputEventStream_1,
                @AssignToField("inputEventStream_2") U inputEventStream_2,
                @AssignToField("mapFunction") SerializableBiFunction<R, Q, T> methodReferenceReflection) {
            super(inputEventStream_1, inputEventStream_2, methodReferenceReflection);
            mapFunction = methodReferenceReflection;
        }

        @Override
        protected void mapOperation() {
            result = mapFunction.apply(getInputEventStream_1().get(), getInputEventStream_2().get());
        }
    }

    public static class BinaryMapToIntFlowFunction<S extends IntFlowFunction, U extends IntFlowFunction>
            extends BinaryMapFlowFunction<Integer, Integer, Integer, S, U>
            implements IntFlowFunction {

        protected transient int result;
        private final SerializableBiIntFunction mapFunction;

        public BinaryMapToIntFlowFunction(
                @AssignToField("inputEventStream") S inputEventStream_1,
                @AssignToField("inputEventStream_2") U inputEventStream_2,
                @AssignToField("mapFunction") SerializableBiIntFunction methodReferenceReflection) {
            super(inputEventStream_1, inputEventStream_2, methodReferenceReflection);
            mapFunction = methodReferenceReflection;
        }

        @Override
        protected void mapOperation() {
            result = mapFunction.applyAsInt(getInputEventStream_1().getAsInt(), getInputEventStream_2().getAsInt());
        }

        @Override
        public Integer get() {
            return getAsInt();
        }

        @Override
        public int getAsInt() {
            return result;
        }
    }


    public static class BinaryMapToDoubleFlowFunction<S extends DoubleFlowFunction, U extends DoubleFlowFunction>
            extends BinaryMapFlowFunction<Double, Double, Double, S, U>
            implements DoubleFlowFunction {

        protected transient double result;
        private final SerializableBiDoubleFunction mapFunction;

        public BinaryMapToDoubleFlowFunction(
                @AssignToField("inputEventStream") S inputEventStream_1,
                @AssignToField("inputEventStream_2") U inputEventStream_2,
                @AssignToField("mapFunction") SerializableBiDoubleFunction methodReferenceReflection) {
            super(inputEventStream_1, inputEventStream_2, methodReferenceReflection);
            mapFunction = methodReferenceReflection;
        }

        @Override
        protected void mapOperation() {
            result = mapFunction.applyAsDouble(getInputEventStream_1().getAsDouble(), getInputEventStream_2().getAsDouble());
        }

        @Override
        public Double get() {
            return getAsDouble();
        }

        @Override
        public double getAsDouble() {
            return result;
        }
    }


    public static class BinaryMapToLongFlowFunction<S extends LongFlowFunction, U extends LongFlowFunction>
            extends BinaryMapFlowFunction<Long, Long, Long, S, U>
            implements LongFlowFunction {

        protected transient long result;
        private final SerializableBiLongFunction mapFunction;

        public BinaryMapToLongFlowFunction(
                @AssignToField("inputEventStream") S inputEventStream_1,
                @AssignToField("inputEventStream_2") U inputEventStream_2,
                @AssignToField("mapFunction") SerializableBiLongFunction methodReferenceReflection) {
            super(inputEventStream_1, inputEventStream_2, methodReferenceReflection);
            mapFunction = methodReferenceReflection;
        }

        @Override
        protected void mapOperation() {
            result = mapFunction.applyAsLong(getInputEventStream_1().getAsLong(), getInputEventStream_2().getAsLong());
        }

        @Override
        public Long get() {
            return getAsLong();
        }

        @Override
        public long getAsLong() {
            return result;
        }
    }
}
