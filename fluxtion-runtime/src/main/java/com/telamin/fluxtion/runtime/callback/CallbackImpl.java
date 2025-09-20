/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.callback;

import com.telamin.fluxtion.runtime.annotations.Initialise;
import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import com.telamin.fluxtion.runtime.flowfunction.TriggeredFlowFunction;
import com.telamin.fluxtion.runtime.node.AbstractEventHandlerNode;
import com.telamin.fluxtion.runtime.node.NamedNode;
import lombok.ToString;

import java.util.Iterator;
import java.util.function.BooleanSupplier;

@ToString
public class CallbackImpl<R, T extends CallbackEvent<?>> extends AbstractEventHandlerNode<CallbackEvent>
        implements TriggeredFlowFunction<R>, NamedNode, Callback<R>, EventDispatcher {
    private final int callbackId;
    @Inject
    public DirtyStateMonitor dirtyStateMonitor;
    @Inject
    private final CallbackDispatcher callBackDispatcher;
    private CallbackEvent<R> event;
    private BooleanSupplier dirtyStateSupplier;

    public CallbackImpl(int callbackId, CallbackDispatcher callBackDispatcher) {
        super(callbackId);
        this.callbackId = callbackId;
        this.callBackDispatcher = callBackDispatcher;
    }

    public CallbackImpl(int callbackId) {
        this(callbackId, null);
    }

    @Override
    public Class<CallbackEvent> eventClass() {
        return CallbackEvent.class;
    }

    @Override
    public boolean onEvent(CallbackEvent e) {
        event = e;
        return true;
    }

    @Override
    public boolean hasChanged() {
        return dirtyStateSupplier.getAsBoolean();
    }

    @Initialise
    public void init() {
        dirtyStateSupplier = dirtyStateMonitor.dirtySupplier(this);
    }

    @Override
    public void parallel() {

    }

    @Override
    public boolean parallelCandidate() {
        return false;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public R get() {
        return event.getData();
    }

    @Override
    public void setUpdateTriggerNode(Object updateTriggerNode) {
    }

    @Override
    public void setPublishTriggerNode(Object publishTriggerNode) {
    }

    @Override
    public void setResetTriggerNode(Object resetTriggerNode) {
    }

    @Override
    public void setPublishTriggerOverrideNode(Object publishTriggerOverrideNode) {
    }

    @Override
    public void fireCallback() {
        callBackDispatcher.fireCallback(callbackId);
    }

    @Override
    public void fireCallback(R data) {
        callBackDispatcher.fireCallback(callbackId, data);
    }

    @Override
    public void fireCallback(Iterator<R> dataIterator) {
        callBackDispatcher.fireIteratorCallback(callbackId, dataIterator);
    }

    @Override
    public void fireNewEventCycle(R data) {
        callBackDispatcher.processAsNewEventCycle(event);
    }

    @Override
    public void processReentrantEvent(Object event) {
        callBackDispatcher.processReentrantEvent(event);
    }

    @Override
    public void processReentrantEvents(Iterable<Object> iterator) {
        callBackDispatcher.processReentrantEvents(iterator);
    }

    @Override
    public void queueReentrantEvent(Object event) {
        callBackDispatcher.queueReentrantEvent(event);
    }

    @Override
    public void processAsNewEventCycle(Object event) {
        callBackDispatcher.processAsNewEventCycle(event);
    }
}
