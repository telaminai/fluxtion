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
import com.telamin.fluxtion.runtime.flowfunction.DoubleFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.IntFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.LongFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableDoubleFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableIntFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableLongFunction;

public class FilterFlowFunction<T, S extends FlowFunction<T>> extends AbstractFlowFunction<T, T, S> {

    final SerializableFunction<T, Boolean> filterFunction;
    transient final String auditInfo;

    public FilterFlowFunction(S inputEventStream, SerializableFunction<T, Boolean> filterFunction) {
        super(inputEventStream, filterFunction);
        this.filterFunction = filterFunction;
        auditInfo = filterFunction.method().getDeclaringClass().getSimpleName() + "->" + filterFunction.method().getName();
    }

    @OnTrigger
    public boolean filter() {
        boolean filter = isPublishTriggered() || filterFunction.apply(getInputEventStream().get());
        boolean fireNotification = filter & fireEventUpdateNotification();
        auditLog.info("filterFunction", auditInfo);
        auditLog.info("filterPass", filter);
        auditLog.info("publishToChild", fireNotification);
        return fireNotification;
    }

    @Override
    public T get() {
        return getInputEventStream().get();
    }


    public static class IntFilterFlowFunction extends AbstractFlowFunction<Integer, Integer, IntFlowFunction> implements IntFlowFunction {

        final SerializableIntFunction<Boolean> filterFunction;
        transient final String auditInfo;

        public IntFilterFlowFunction(IntFlowFunction inputEventStream, SerializableIntFunction<Boolean> filterFunction) {
            super(inputEventStream, filterFunction);
            this.filterFunction = filterFunction;
            auditInfo = filterFunction.method().getDeclaringClass().getSimpleName() + "->" + filterFunction.method().getName();
        }

        @OnTrigger
        public boolean filter() {
            boolean filter = isPublishTriggered() || filterFunction.apply(getInputEventStream().getAsInt());
            boolean fireNotification = filter & fireEventUpdateNotification();
            auditLog.info("filterFunction", auditInfo);
            auditLog.info("filterPass", filter);
            auditLog.info("publishToChild", fireNotification);
            return fireNotification;
        }

        @Override
        public Integer get() {
            return getAsInt();
        }

        @Override
        public int getAsInt() {
            return getInputEventStream().getAsInt();
        }
    }


    public static class DoubleFilterFlowFunction extends AbstractFlowFunction<Double, Double, DoubleFlowFunction> implements DoubleFlowFunction {

        final SerializableDoubleFunction<Boolean> filterFunction;
        transient final String auditInfo;

        public DoubleFilterFlowFunction(DoubleFlowFunction inputEventStream, SerializableDoubleFunction<Boolean> filterFunction) {
            super(inputEventStream, filterFunction);
            this.filterFunction = filterFunction;
            auditInfo = filterFunction.method().getDeclaringClass().getSimpleName() + "->" + filterFunction.method().getName();
        }

        @OnTrigger
        public boolean filter() {
            boolean filter = isPublishTriggered() || filterFunction.apply(getInputEventStream().getAsDouble());
            boolean fireNotification = filter & fireEventUpdateNotification();
            auditLog.info("filterFunction", auditInfo);
            auditLog.info("filterPass", filter);
            auditLog.info("publishToChild", fireNotification);
            return fireNotification;
        }

        @Override
        public Double get() {
            return getAsDouble();
        }

        @Override
        public double getAsDouble() {
            return getInputEventStream().getAsDouble();
        }
    }


    public static class LongFilterFlowFunction extends AbstractFlowFunction<Long, Long, LongFlowFunction> implements LongFlowFunction {

        final SerializableLongFunction<Boolean> filterFunction;
        transient final String auditInfo;

        public LongFilterFlowFunction(LongFlowFunction inputEventStream, SerializableLongFunction<Boolean> filterFunction) {
            super(inputEventStream, filterFunction);
            this.filterFunction = filterFunction;
            auditInfo = filterFunction.method().getDeclaringClass().getSimpleName() + "->" + filterFunction.method().getName();
        }

        @OnTrigger
        public boolean filter() {
            boolean filter = isPublishTriggered() || filterFunction.apply(getInputEventStream().getAsLong());
            boolean fireNotification = filter & fireEventUpdateNotification();
            auditLog.info("filterFunction", auditInfo);
            auditLog.info("filterPass", filter);
            auditLog.info("publishToChild", fireNotification);
            return fireNotification;
        }

        @Override
        public Long get() {
            return getAsLong();
        }

        @Override
        public long getAsLong() {
            return getInputEventStream().getAsLong();
        }
    }

}
