/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.output;

import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.annotations.runtime.ServiceDeregistered;
import com.telamin.fluxtion.runtime.annotations.runtime.ServiceRegistered;
import com.telamin.fluxtion.runtime.node.SingleNamedNode;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public class SinkPublisher<T> extends SingleNamedNode {

    private transient final String filterString;
    private Consumer<T> sink;
    private IntConsumer intConsumer;

    private LongConsumer longConsumer;

    private DoubleConsumer doubleConsumer;

    public SinkPublisher(@AssignToField("name") String filterString) {
        super(filterString);
        this.filterString = filterString;
    }

    @OnEventHandler(filterVariable = "filterString", propagate = false)
    public void sinkRegistration(SinkRegistration<T> sinkRegistration) {
        sink = sinkRegistration.getConsumer();
        intConsumer = sinkRegistration.getIntConsumer();
        longConsumer = sinkRegistration.getLongConsumer();
        doubleConsumer = sinkRegistration.getDoubleConsumer();
    }

    @OnEventHandler(filterVariable = "filterString", propagate = false)
    public void unregisterSink(SinkDeregister sinkDeregister) {
        sink = null;
        intConsumer = null;
        longConsumer = null;
        doubleConsumer = null;
    }

    @ServiceRegistered
    public void messageSinkRegistered(MessageSink<T> messageSink, String name) {
        if (name.equals(getName())) {
            auditLog.info("registeredMessageSink", name);
            sink = messageSink;
        }
    }

    @ServiceDeregistered
    public void messageSinkDeregistered(MessageSink<T> messageSink, String name) {
        if (name.equals(getName())) {
            auditLog.info("deregisteredMessageSink", name);
            sink = null;
        }
    }

    public void publish(T publishItem) {
        if (sink != null)
            sink.accept(publishItem);
    }

    public void publishInt(int value) {
        if (intConsumer != null)
            intConsumer.accept(value);
    }

    public void publishDouble(double value) {
        if (doubleConsumer != null)
            doubleConsumer.accept(value);
    }

    public void publishLong(long value) {
        if (longConsumer != null)
            longConsumer.accept(value);
    }

}
