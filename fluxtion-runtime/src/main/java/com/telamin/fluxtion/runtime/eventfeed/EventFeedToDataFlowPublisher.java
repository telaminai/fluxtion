/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.eventfeed;

import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.annotations.feature.Experimental;
import com.telamin.fluxtion.runtime.event.NamedFeedEvent;
import com.telamin.fluxtion.runtime.event.NamedFeedEventImpl;
import lombok.extern.java.Log;
import org.agrona.collections.ArrayUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Helper class that manages dispatching events to registered {@link DataFlow}'s. Supports caching of events for
 * listeners that register after the events have already been published.
 *
 * @param <T>
 */
@Experimental
@Log
public class EventFeedToDataFlowPublisher<T> {

    private DataFlow[] dataFlows = new DataFlow[0];
    private final String feedName;
    private final boolean cacheEventLog;
    private final List<NamedFeedEvent<?>> events = new ArrayList<>();
    private Function<? super T, ?> valueMapper = Function.identity();
    private long sequenceNumber = 0;

    public EventFeedToDataFlowPublisher(String feedName, boolean cacheEventLog) {
        this.feedName = feedName;
        this.cacheEventLog = cacheEventLog;
    }

    public void setValueMapper(Function<? super T, ?> valueMapper) {
        log.info(feedName + " setting valueMapper: " + valueMapper);
        this.valueMapper = valueMapper;
    }

    public void addDataFlowReceiver(DataFlow dataFlow) {
        log.info(feedName + " adding dataflow: " + dataFlow);
        for (DataFlow dataFlow1 : dataFlows) {
            if (dataFlow1.equals(dataFlow)) {
                return;
            }
        }

        dataFlows = ArrayUtil.add(dataFlows, dataFlow);
        log.info(feedName + " remaining dataflows: " + dataFlows.length);

        for (NamedFeedEvent<?> event : events) {
            dataFlow.onEvent(event);
        }
    }

    public void removeDataFlowReceiver(DataFlow dataFlow) {
        log.info(feedName + " removing dataflow: " + dataFlow);
        dataFlows = ArrayUtil.remove(dataFlows, dataFlow);
        log.info(feedName + " remaining dataflows: " + dataFlows.length);
    }

    public List<NamedFeedEvent<?>> getEventLog() {
        return events;
    }

    public void publish(T event, boolean wrapEvent) {
        Object toPublish = valueMapper.apply(event);
        if (toPublish == null) {
            return;
        }
        if (wrapEvent) {
            var namedEvent = new NamedFeedEventImpl<>(feedName, null, sequenceNumber++, toPublish);
            if (cacheEventLog) {
                events.add(namedEvent);
            }
            toPublish = namedEvent;
        }
        log.fine(() -> feedName + " publishing event " + event);
        for (DataFlow dataFlow : dataFlows) {
            log.finer(() -> feedName + " publishing event " + event + " to dataflow " + dataFlow);
            dataFlow.onEvent(toPublish);
        }
    }
}
