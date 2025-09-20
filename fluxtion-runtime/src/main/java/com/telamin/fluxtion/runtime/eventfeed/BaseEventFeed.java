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
import com.telamin.fluxtion.runtime.node.EventSubscription;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.function.Function;

/**
 * Custom {@link EventFeedAgent}'s developers can extend this class to simplify creating a new {@link EventFeedAgent}
 *
 * @param <T>
 */
@Experimental
public abstract class BaseEventFeed<T> implements EventFeedAgent<T> {

    private final EventFeedToDataFlowPublisher<T> output;
    @Getter
    private final boolean cacheEventLog;
    @Getter
    private final ReadStrategy readStrategy;
    @Getter
    private final String feedName;
    @Getter
    private final boolean broadcast;
    @Getter
    @Setter
    private boolean wrapEvent = true;

    public BaseEventFeed(String feedName) {
        this(feedName, false, ReadStrategy.EARLIEST, false);
    }

    public BaseEventFeed(
            String feedName,
            boolean cacheEventLog,
            ReadStrategy readStrategy,
            boolean broadcast) {
        this.cacheEventLog = cacheEventLog;
        this.readStrategy = readStrategy;
        this.feedName = feedName;
        this.broadcast = broadcast;
        output = new EventFeedToDataFlowPublisher<>(feedName, cacheEventLog);
    }

    public BaseEventFeed(
            String feedName,
            boolean cacheEventLog,
            ReadStrategy readStrategy,
            boolean broadcast,
            EventFeedToDataFlowPublisher<T> output) {
        this.output = output;
        this.cacheEventLog = cacheEventLog;
        this.readStrategy = readStrategy;
        this.feedName = feedName;
        this.broadcast = broadcast;
    }

    @Override
    public final void registerSubscriber(DataFlow subscriber) {
        if (broadcast) {
            output.addDataFlowReceiver(subscriber);
        }
    }

    @Override
    public final void subscribe(DataFlow subscriber, EventSubscription<?> subscriptionId) {
        if (!broadcast && validSubscription(subscriber, subscriptionId)) {
            output.addDataFlowReceiver(subscriber);
        }
    }

    @Override
    public final void unSubscribe(DataFlow subscriber, EventSubscription<?> subscriptionId) {
        if (!broadcast && validSubscription(subscriber, subscriptionId)) {
            output.removeDataFlowReceiver(subscriber);
        }
    }

    @Override
    public final void removeAllSubscriptions(DataFlow subscriber) {
        output.removeDataFlowReceiver(subscriber);
    }

    public void publish(T event) {
        output.publish(event, wrapEvent);
    }

    @Override
    public abstract int doWork() throws Exception;

    @Override
    public final String roleName() {
        return feedName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public NamedFeedEvent<?>[] eventLog() {
        List<NamedFeedEvent<?>> eventLog = output.getEventLog();
        return eventLog.toArray(new NamedFeedEvent[0]);
    }

    public BaseEventFeed<T> setValueMapper(Function<? super T, ?> valueMapper) {
        output.setValueMapper(valueMapper);
        return this;
    }

    protected abstract boolean validSubscription(DataFlow subscriber, EventSubscription<?> subscriptionId);
}
