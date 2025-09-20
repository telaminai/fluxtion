/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.input;

import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.annotations.TearDown;
import com.telamin.fluxtion.runtime.annotations.builder.FluxtionIgnore;
import com.telamin.fluxtion.runtime.annotations.runtime.ServiceDeregistered;
import com.telamin.fluxtion.runtime.annotations.runtime.ServiceRegistered;
import com.telamin.fluxtion.runtime.event.NamedFeedEvent;
import com.telamin.fluxtion.runtime.node.EventSubscription;
import com.telamin.fluxtion.runtime.node.NamedNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SubscriptionManagerNode implements SubscriptionManager, NamedNode {

    //feeds
    private final transient List<EventFeed> registeredFeeds = new ArrayList<>();
    private final transient Map<String, NamedFeed> registeredNameEventFeedMap = new HashMap<>();
    //subscriptions
    private final transient Map<Object, Integer> subscriptionMap = new HashMap<>();
    private final transient Map<EventSubscription<?>, Integer> namedFeedSubscriptionMap = new HashMap<>();
    @FluxtionIgnore
    private DataFlow eventProcessor = DataFlow.NULL_EVENTHANDLER;

    public void setSubscribingEventProcessor(DataFlow eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    public void addEventProcessorFeed(EventFeed eventFeed) {
        if (!registeredFeeds.contains(eventFeed)) {
            eventFeed.registerSubscriber(eventProcessor);
            registeredFeeds.add(eventFeed);
            subscriptionMap.keySet().forEach(e -> eventFeed.subscribe(eventProcessor, e));
        }
    }

    public void removeEventProcessorFeed(EventFeed eventProcessorFeed) {
        registeredFeeds.remove(eventProcessorFeed);
    }

    @Override
    public void subscribe(Object subscriptionId) {
        subscriptionMap.compute(subscriptionId, (k, v) -> {
            if (v == null) {
                registeredFeeds.forEach(e -> e.subscribe(eventProcessor, subscriptionId));
                return 1;
            }
            return ++v;
        });
    }

    @Override
    public void unSubscribe(Object subscriptionId) {
        subscriptionMap.computeIfPresent(subscriptionId, (o, i) -> {
            if (--i < 1) {
                registeredFeeds.forEach(e -> e.unSubscribe(eventProcessor, subscriptionId));
                return null;
            }
            return i;
        });
    }

    @ServiceRegistered
    public void registerEventFeedService(NamedFeed eventFeed, String feedName) {
        if (!registeredNameEventFeedMap.containsKey(feedName)) {
            eventFeed.registerSubscriber(eventProcessor);
            registeredNameEventFeedMap.put(feedName, eventFeed);
            namedFeedSubscriptionMap.keySet().forEach(e -> {
                eventFeed.subscribe(eventProcessor, e);
            });
        }
    }

    @ServiceDeregistered
    public void deRegisterEventFeedService(NamedFeed eventFeed, String feedName) {
        registeredNameEventFeedMap.remove(feedName);
    }

    @Override
    public void subscribeToNamedFeed(EventSubscription<?> subscription) {
        namedFeedSubscriptionMap.compute(subscription, (k, v) -> {
            if (v == null) {
                registeredNameEventFeedMap.values().forEach(e -> e.subscribe(eventProcessor, subscription));
                return 1;
            }
            return ++v;
        });
    }

    @Override
    public void subscribeToNamedFeed(String feedName) {
        subscribeToNamedFeed(new EventSubscription<>(feedName, Integer.MAX_VALUE, feedName, NamedFeedEvent.class));
    }

    @Override
    public void unSubscribeToNamedFeed(EventSubscription<?> subscription) {
        namedFeedSubscriptionMap.computeIfPresent(subscription, (k, i) -> {
            if (--i < 1) {
                registeredNameEventFeedMap.values().forEach(e -> e.unSubscribe(eventProcessor, subscription));
                return 1;
            }
            return i;
        });
    }

    @Override
    public void unSubscribeToNamedFeed(String feedName) {
        unSubscribeToNamedFeed(new EventSubscription<>(feedName, Integer.MAX_VALUE, feedName, NamedFeedEvent.class));
    }

    @TearDown
    public void tearDown() {
        registeredFeeds.forEach(e -> e.removeAllSubscriptions(eventProcessor));
        subscriptionMap.clear();
        //
        registeredNameEventFeedMap.values().forEach(e -> e.removeAllSubscriptions(eventProcessor));
        namedFeedSubscriptionMap.clear();
    }

    @Override
    public String getName() {
        return SubscriptionManager.DEFAULT_NODE_NAME;
    }
}