/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.callback;

import com.fluxtion.dataflow.runtime.CloneableDataFlow;
import com.fluxtion.dataflow.runtime.context.DataFlowContext;
import com.fluxtion.dataflow.runtime.context.DataFlowContextListener;
import com.fluxtion.dataflow.runtime.annotations.builder.FluxtionIgnore;
import com.fluxtion.dataflow.runtime.node.EventHandlerNode;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Extend this node to expose instance callback outside the {@link CloneableDataFlow}.
 * Use the protected fireCallback methods to trigger child nodes. Data can be optionally passed into a fireCallback method
 */
public class CallBackNode<R>
        implements
        Callback<R>,
        EventHandlerNode<InstanceCallbackEvent>,
        DataFlowContextListener {

    private final InstanceCallbackEvent event;
    protected R data;
    @FluxtionIgnore
    private final List<R> dataQueue = new ArrayList<>();
    private EventDispatcher dispatcher;

    public CallBackNode(InstanceCallbackEvent event) {
        this.event = event;
    }

    @SneakyThrows
    public CallBackNode() {
        event = InstanceCallbackEvent.nextCallBackEvent();
    }

    @Override
    public void currentContext(DataFlowContext currentContext) {
        dispatcher = currentContext.getEventDispatcher();
    }

    @Override
    public Class<? extends InstanceCallbackEvent> eventClass() {
        return event.getClass();
    }

    @Override
    public <E extends InstanceCallbackEvent> boolean onEvent(E e) {
        data = dataQueue.isEmpty() ? null : dataQueue.remove(0);
        return true;
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
        dispatcher.processReentrantEvent(event);
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
        dispatcher.processAsNewEventCycle(event);
    }

    @Override
    public R get() {
        return data;
    }
}
