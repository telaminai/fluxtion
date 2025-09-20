/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.node;

import com.fluxtion.dataflow.runtime.context.DataFlowContext;
import com.fluxtion.dataflow.runtime.annotations.Initialise;
import com.fluxtion.dataflow.runtime.annotations.TearDown;
import com.fluxtion.dataflow.runtime.annotations.builder.AssignToField;
import com.fluxtion.dataflow.runtime.annotations.builder.Inject;
import com.fluxtion.dataflow.runtime.audit.EventLogNode;
import com.fluxtion.dataflow.runtime.flowfunction.TriggeredFlowFunction;
import com.fluxtion.dataflow.runtime.event.Event;
import com.fluxtion.dataflow.runtime.lifecycle.Lifecycle;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * {@inheritDoc}
 */
public final class DefaultEventHandlerNode<T>
        extends EventLogNode
        implements
        Lifecycle,
        NamedNode,
        EventHandlerNode<T>,
        TriggeredFlowFunction<T> {

    private final int filterId;
    private final String filterString;
    private final Class<T> eventClass;
    private final String name;
    private T event;
    private BooleanSupplier dirtySupplier;
    @Inject
    private final DataFlowContext dataFlowContext;

    public DefaultEventHandlerNode(Class<T> eventClass) {
        this(Event.NO_INT_FILTER, Event.NO_STRING_FILTER, eventClass);
    }

    public DefaultEventHandlerNode(int filterId, Class<T> eventClass) {
        this(filterId, Event.NO_STRING_FILTER, eventClass);
    }

    public DefaultEventHandlerNode(String filterString, Class<T> eventClass) {
        this(Event.NO_INT_FILTER, filterString, eventClass);
    }

    public DefaultEventHandlerNode(
            int filterId,
            String filterString,
            Class<T> eventClass
    ) {
        this.filterId = filterId;
        this.filterString = filterString;
        this.eventClass = eventClass;
        if (filterId != Event.NO_INT_FILTER) {
            name = "handler" + eventClass.getSimpleName() + "_" + filterId;
        } else if (!filterString.equals(Event.NO_STRING_FILTER)) {
            name = "handler" + eventClass.getSimpleName() + "_" + filterString;
        } else {
            name = "handler" + eventClass.getSimpleName();
        }
        dataFlowContext = null;
    }

    public DefaultEventHandlerNode(
            int filterId,
            @AssignToField("filterString") String filterString,
            Class<T> eventClass,
            @AssignToField("name") String name,
            DataFlowContext dataFlowContext) {
        this.dataFlowContext = dataFlowContext;
        this.filterId = filterId;
        this.filterString = filterString;
        this.eventClass = eventClass;
        this.name = name;
    }

    @Override
    public void parallel() {

    }

    @Override
    public boolean parallelCandidate() {
        return false;
    }

    @Override
    public int filterId() {
        return filterId;
    }

    @Override
    public <E extends T> boolean onEvent(E e) {
        auditLog.info("inputEvent", e.getClass().getSimpleName());
        this.event = e;
        return true;
    }

    @Override
    public String filterString() {
        return filterString;
    }

    @Override
    public Class<T> eventClass() {
        return eventClass;
    }


    @Override
    public boolean hasChanged() {
        return dirtySupplier.getAsBoolean();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + this.filterId;
        hash = 13 * hash + Objects.hashCode(this.eventClass);
        hash = 13 * hash + Objects.hashCode(this.filterString);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DefaultEventHandlerNode<?> other = (DefaultEventHandlerNode<?>) obj;
        if (this.filterId != other.filterId) {
            return false;
        }
        if (this.filterString != other.filterString) {
            return false;
        }
        if (!Objects.equals(this.eventClass, other.eventClass)) {
            return false;
        }
        return true;
    }

    @Initialise
    @Override
    public void init() {
        dirtySupplier = dataFlowContext.getDirtyStateMonitor().dirtySupplier(this);
    }

    @Override
    @TearDown
    public void tearDown() {
    }

    @Override
    public T get() {
        return event;
    }

    @Override
    public void setUpdateTriggerNode(Object updateTriggerNode) {
        //do nothing
    }

    @Override
    public void setPublishTriggerNode(Object publishTriggerNode) {
        //do nothing
    }

    @Override
    public void setResetTriggerNode(Object resetTriggerNode) {
        //do nothing
    }

    @Override
    public void setPublishTriggerOverrideNode(Object publishTriggerOverrideNode) {
    }

    @Override
    public String getName() {
        return name;
    }
}
