/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.callback;

import com.telamin.fluxtion.runtime.CloneableDataFlow;
import com.telamin.fluxtion.runtime.context.buildtime.GeneratorNodeCollection;
import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import com.telamin.fluxtion.runtime.annotations.builder.FluxtionIgnore;
import com.telamin.fluxtion.runtime.event.Event;
import com.telamin.fluxtion.runtime.node.BaseNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Extend this node to expose instance callback outside the {@link CloneableDataFlow}.
 * Use the protected fireCallback methods to trigger child nodes. Data can be optionally passed into a fireCallback method
 */
public abstract class AbstractCallbackNode<R> extends BaseNode implements Event, Callback<R> {

    public static int instanceFilterCounter = 0;
    private final int filterId;
    protected R data;
    @FluxtionIgnore
    private final List<R> dataQueue = new ArrayList<>();

    public AbstractCallbackNode() {
        filterId = GeneratorNodeCollection.nextId(instanceFilterCounter++);
    }

    public AbstractCallbackNode(int filterId) {
        this.filterId = filterId;
    }

    @OnEventHandler(filterVariable = "filterId")
    public boolean trigger(AbstractCallbackNode<R> callbackNode) {
        boolean matchSource = callbackNode == this;
        if (matchSource) {
            data = dataQueue.isEmpty() ? null : dataQueue.remove(0);
        }
        return matchSource;
    }

    /**
     * Trigger a callback calculation with this node as the root of the event cycle. The value of {@link #data} will be
     * null when this node triggers
     */
    @Override
    public void fireCallback() {
        fireCallback((R) null);
    }

    /**
     * Trigger a callback calculation with this node as the root of the event cycle. The value of {@link #data}
     * will be the value passed in when this node triggers
     *
     * @param data the data to pass into the callback
     */
    @Override
    public void fireCallback(R data) {
        dataQueue.add(data);
        getContext().processReentrantEvent(this);
    }

    /**
     * Trigger a callback calculation with this node as the root of the event cycle. Fires for every data item present in
     * the iterator. The value of {@link #data} when triggering will be the value {@link Iterator#next()} returns
     *
     * @param dataIterator
     */
    @Override
    public void fireCallback(Iterator<R> dataIterator) {
        while (dataIterator.hasNext()) {
            R nextItem = dataIterator.next();
            fireCallback(nextItem);
        }
    }

    /**
     * Fires a new event cycle, with this callback executed after any queued events have completed. The value of {@link #data}
     * will be the value passed in when this node triggers
     *
     * @param data
     */
    @Override
    public void fireNewEventCycle(R data) {
        dataQueue.add(data);
        getContext().processAsNewEventCycle(this);
    }

    @Override
    public R get() {
        return data;
    }

    @Override
    public int filterId() {
        return filterId;
    }
}
