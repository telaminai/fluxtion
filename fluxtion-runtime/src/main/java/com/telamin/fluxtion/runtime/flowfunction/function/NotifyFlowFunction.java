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
import com.telamin.fluxtion.runtime.annotations.PushReference;
import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import com.telamin.fluxtion.runtime.flowfunction.DoubleFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.IntFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.LongFlowFunction;
import com.telamin.fluxtion.runtime.node.NodeNameLookup;
import lombok.ToString;

import java.util.Objects;

@ToString
public class NotifyFlowFunction<T, S extends FlowFunction<T>> extends AbstractFlowFunction<T, T, S> {

    @PushReference
    private final Object target;
    private final transient String auditInfo;
    private String instanceNameToNotify;
    @Inject
    @NoTriggerReference
    public NodeNameLookup nodeNameLookup;

    public NotifyFlowFunction(S inputEventStream, Object target) {
        super(inputEventStream, null);
        this.target = target;
        auditInfo = target.getClass().getSimpleName();
    }

    protected void initialise() {
        instanceNameToNotify = nodeNameLookup.lookupInstanceName(target);
    }

    @OnTrigger
    public boolean notifyChild() {
        auditLog.info("notifyClass", auditInfo);
        auditLog.info("notifyInstance", instanceNameToNotify);
        return fireEventUpdateNotification();
    }

    @Override
    public T get() {
        return getInputEventStream().get();
    }

    @ToString
    public static class IntNotifyFlowFunction extends NotifyFlowFunction<Integer, IntFlowFunction> implements IntFlowFunction {

        public IntNotifyFlowFunction(IntFlowFunction inputEventStream, Object target) {
            super(inputEventStream, target);
        }

        @Override
        public int getAsInt() {
            return getInputEventStream().getAsInt();
        }
    }

    @ToString
    public static class DoubleNotifyFlowFunction extends NotifyFlowFunction<Double, DoubleFlowFunction> implements DoubleFlowFunction {

        public DoubleNotifyFlowFunction(DoubleFlowFunction inputEventStream, Object target) {
            super(inputEventStream, target);
        }

        @Override
        public double getAsDouble() {
            return getInputEventStream().getAsDouble();
        }
    }

    @ToString
    public static class LongNotifyFlowFunction extends NotifyFlowFunction<Long, LongFlowFunction> implements LongFlowFunction {

        public LongNotifyFlowFunction(LongFlowFunction inputEventStream, Object target) {
            super(inputEventStream, target);
        }

        @Override
        public long getAsLong() {
            return getInputEventStream().getAsLong();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotifyFlowFunction)) return false;
        NotifyFlowFunction<?, ?> that = (NotifyFlowFunction<?, ?>) o;
        return target.equals(that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target);
    }
}
