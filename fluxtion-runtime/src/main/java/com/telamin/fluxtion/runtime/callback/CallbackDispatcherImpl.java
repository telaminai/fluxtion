/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.callback;

import com.telamin.fluxtion.runtime.node.NamedNode;
import lombok.ToString;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.function.BooleanSupplier;

@ToString
public class CallbackDispatcherImpl implements EventProcessorCallbackInternal, NamedNode, DirtyStateMonitor {

    public InternalEventProcessor eventProcessor;
    // W2: declared as the concrete type — an interface-typed field on this path is not
    // devirtualised by closed-world AOT without profiles (M50 §3.3).
    // W3: BooleanSupplier, not Supplier<Boolean> — every wrapper's dispatch() already
    // returns primitive boolean, so the generic queue boxed on every callback.
    ArrayDeque<BooleanSupplier> myStack = new ArrayDeque<>();
    private boolean dispatching = false;

    /**
     * M50/W1 — every queueing path funnels through here so the processor's flag cannot drift from the
     * queue. Adding a queueing method without calling this is the one way to break the optimisation,
     * and {@code CallbackDispatcherPendingTest} fails if a public queueing method does not mark.
     */
    private void markPending() {
        if (eventProcessor != null) {
            eventProcessor.callbacksPending(true);
        }
    }

    @Override
    public void dispatchQueuedCallbacks() {
        // W2: the common case is an empty queue on every event. Return before touching
        // `dispatching`, which can only be true while the loop below is running, so the
        // unconditional store it replaces was always redundant here.
        if (eventProcessor == null || myStack.isEmpty()) {
            return;
        }
        while (!myStack.isEmpty()) {
            dispatching = true;
            BooleanSupplier callBackItem = myStack.peekFirst();
            if (!callBackItem.getAsBoolean()) {
                myStack.remove(callBackItem);
            }
        }
        dispatching = false;
        eventProcessor.callbacksPending(false);
    }

    @Override
    public void fireCallback(int id) {
        SingleCallBackWrapper<Object> callBackWrapper = new SingleCallBackWrapper<>();
        callBackWrapper.setFilterId(id);
        myStack.add(callBackWrapper::dispatch);
        markPending();
    }

    @Override
    public <T> void fireCallback(int id, T item) {
        //System.out.println("firing callback id:" + id + " item:" + item);
        SingleCallBackWrapper<T> callBackWrapper = new SingleCallBackWrapper<>();
        callBackWrapper.setFilterId(id);
        callBackWrapper.setData(item);
        myStack.add(callBackWrapper::dispatch);
        markPending();
    }

    @Override
    public <R> void fireIteratorCallback(int callbackId, Iterator<R> dataIterator) {
        IteratingCallbackWrapper<R> callBackWrapper = new IteratingCallbackWrapper<>();
        callBackWrapper.setFilterId(callbackId);
        callBackWrapper.dataIterator = dataIterator;
        if (dispatching) {
            //System.out.println("DISPATCHING adding iterator to FRONT of callback queue id:" + callbackId);
            myStack.addFirst(callBackWrapper::dispatch);
        } else {
            //System.out.println("adding iterator to BACK of callback queue id:" + callbackId);
            myStack.add(callBackWrapper::dispatch);
        }
        markPending();
    }

    @Override
    public void processReentrantEvent(Object event) {
        SingleEventPublishWrapper<Object> callBackWrapper = new SingleEventPublishWrapper<>();
        callBackWrapper.data = event;
        myStack.addFirst(callBackWrapper::dispatch);
        markPending();
    }

    @Override
    public void processReentrantEvents(Iterable<Object> iterable) {
        IteratingEventPublishWrapper publishingWrapper = new IteratingEventPublishWrapper();
        publishingWrapper.dataIterator = iterable.iterator();
        myStack.addFirst(publishingWrapper::dispatch);
        markPending();
    }

    @Override
    public void queueReentrantEvent(Object event) {
        SingleEventPublishWrapper<Object> callBackWrapper = new SingleEventPublishWrapper<>();
        callBackWrapper.data = event;
        myStack.add(callBackWrapper::dispatch);
        markPending();
    }

    @Override
    public void processAsNewEventCycle(Object event) {
        eventProcessor.onEvent(event);
    }

    @Override
    public String getName() {
        return CallbackDispatcher.DEFAULT_NODE_NAME;
    }

    @Override
    public boolean isDirty(Object node) {
        return node != null && eventProcessor.isDirty(node);
    }

    @Override
    public BooleanSupplier dirtySupplier(Object node) {
        return eventProcessor.dirtySupplier(node);
    }

    @Override
    public void markDirty(Object node) {
        eventProcessor.setDirty(node, true);
    }

    //    @Override
    public <T> T getNodeById(String id) throws NoSuchFieldException {
        return eventProcessor.getNodeById(id);
    }

    @Override
    public void setEventProcessor(InternalEventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    @ToString(callSuper = true)
    private class SingleCallBackWrapper<T> extends CallbackEvent<T> {

        private final CallbackEvent<T> callbackEvent = new CallbackEvent<>();

        boolean dispatch() {
            //System.out.println("dispatching this id:" + filterId);
            callbackEvent.setData(getData());
            callbackEvent.setFilterId(getFilterId());
            eventProcessor.onEventInternal(callbackEvent);
            setData(null);
            setFilterId(Integer.MAX_VALUE);
            return false;
        }
    }

    @ToString(callSuper = true)
    private class SingleEventPublishWrapper<T> {

        T data;

        boolean dispatch() {
            eventProcessor.onEventInternal(data);
            return false;
        }
    }

    @ToString(callSuper = true)
    private class IteratingCallbackWrapper<T> extends CallbackEvent<T> {
        Iterator<T> dataIterator;
        private final CallbackEvent<T> callbackEvent = new CallbackEvent<>();

        boolean dispatch() {
            //System.out.println("dispatching this id:" + filterId);
            if (dataIterator.hasNext()) {
                callbackEvent.setData(dataIterator.next());
                callbackEvent.setFilterId(getFilterId());
                eventProcessor.onEventInternal(callbackEvent);
                return true;
            }
            return false;
        }
    }

    private class IteratingEventPublishWrapper {
        Iterator<Object> dataIterator;

        boolean dispatch() {
            if (dataIterator.hasNext()) {
                eventProcessor.onEventInternal(dataIterator.next());
                return true;
            }
            return false;
        }
    }

}
