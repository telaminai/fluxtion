/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.connector;

import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.annotations.feature.Experimental;
import com.telamin.fluxtion.runtime.eventfeed.EventFeedAgent;
import com.telamin.fluxtion.runtime.input.NamedFeed;
import com.telamin.fluxtion.runtime.lifecycle.Lifecycle;
import com.telamin.fluxtion.runtime.output.SinkRegistration;
import lombok.extern.java.Log;
import org.agrona.collections.ArrayUtil;
import org.agrona.concurrent.OneToOneConcurrentArrayQueue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


/**
 * Internal class that manages multiple {@link EventFeedAgent}'s and {@link DataFlow}'s lifecycles within a Thread
 * safe context
 */
@Experimental
@Log
public class EventFeedToDataFlowAgent extends WorkDynamicCompositeAgent {

    private final OneToOneConcurrentArrayQueue<EventFeedAgent<?>> feedToAddList;
    private final OneToOneConcurrentArrayQueue<EventFeedAgent<?>> feedToRemoveList;
    private EventFeedAgent<?>[] feeds;
    private final OneToOneConcurrentArrayQueue<DataFlow> dataFlowsToAdd;
    private final OneToOneConcurrentArrayQueue<DataFlow> dataFlowsToRemove;
    private final OneToOneConcurrentArrayQueue<SinkRegistration<?>> sinksToAdd;
    private final OneToOneConcurrentArrayQueue<String> sinksToRemove;
    private DataFlow[] dataFlows;
    private final AtomicBoolean doWorkActive = new AtomicBoolean(false);

    public EventFeedToDataFlowAgent(String roleName) {
        super(roleName);
        feedToAddList = new OneToOneConcurrentArrayQueue<>(128);
        feedToRemoveList = new OneToOneConcurrentArrayQueue<>(128);
        feeds = new EventFeedAgent[0];
        dataFlowsToAdd = new OneToOneConcurrentArrayQueue<>(128);
        dataFlowsToRemove = new OneToOneConcurrentArrayQueue<>(128);
        sinksToAdd = new OneToOneConcurrentArrayQueue<>(128);
        sinksToRemove = new OneToOneConcurrentArrayQueue<>(128);
        dataFlows = new DataFlow[0];
    }

    public void addFeed(EventFeedAgent<?> feed) {
        feedToAddList.add(feed);
//        checkForRegistrationUpdates();
    }

    public void removeFeed(EventFeedAgent<?> feed) {
        feedToRemoveList.add(feed);
//        checkForRegistrationUpdates();
    }

    public void addDataFlow(DataFlow dataFlow) {
        dataFlowsToAdd.add(dataFlow);
        checkForRegistrationUpdates();
    }

    public void removeDataFlow(DataFlow dataFlow) {
        dataFlowsToRemove.add(dataFlow);
//        checkForRegistrationUpdates();
    }

    public <T> void addSink(String id, Consumer<T> sink) {
        sinksToAdd.add(SinkRegistration.sink(id, sink));
//        checkForRegistrationUpdates();
    }

    public <T> void removeSink(String id) {
        sinksToRemove.add(id);
//        checkForRegistrationUpdates();
    }

    @Override
    public int doWork() throws Exception {
        try {
            doWorkActive.set(true);
            checkForRegistrationUpdates();
            return super.doWork();
        } finally {
            doWorkActive.set(false);
        }
    }

    @Override
    protected void postWork() {
        checkForRegistrationUpdates();
    }

    @Override
    public void onClose() {
        try {
            doWorkActive.set(true);
            checkForRegistrationUpdates();
            super.onClose();
        } finally {
            doWorkActive.set(false);
        }
    }

    private void checkForRegistrationUpdates() {
        if (!doWorkActive.get()) {
            return;
        }
        log.finest("Checking for registration updates");
        dataFlowsToAdd.drain(dataFlow -> {
            dataFlows = ArrayUtil.add(dataFlows, dataFlow);

            for (EventFeedAgent<?> feed : feeds) {
                log.info(() -> "register subscriber " + dataFlow + " with feed " + feed);
                feed.registerSubscriber(dataFlow);
                dataFlow.registerService(feed, NamedFeed.class, feed.roleName());
            }
        });

        dataFlowsToRemove.drain(dataFlow -> {
            dataFlows = ArrayUtil.remove(dataFlows, dataFlow);

            for (EventFeedAgent<?> feed : feeds) {
                log.info(() -> "deregister subscriber " + dataFlow + " with feed " + feed);
                dataFlow.deRegisterService(feed, NamedFeed.class);
                feed.removeAllSubscriptions(dataFlow);
            }
        });

        sinksToAdd.drain(sink -> {
            for (DataFlow dataFlow : dataFlows) {
                log.info(() -> "adding sink " + sink + " to dataflow " + dataFlow);

                Consumer<?> consumer = sink.getConsumer();
                if (consumer instanceof Lifecycle lifecycle) {
                    lifecycle.init();
                    lifecycle.start();
                }
                dataFlow.addSink(sink.filterString(), consumer);
            }
        });

        sinksToRemove.drain(sinkId -> {
            for (DataFlow dataFlow : dataFlows) {
                log.info(() -> "removing sink " + sinkId + " to dataflow " + dataFlow);
                dataFlow.removeSink(sinkId);
            }
        });

        EventFeedAgent<?> feedToAdd = feedToAddList.poll();
        if (feedToAdd != null) {
            log.info(() -> "add feed " + feedToAdd);
            boolean added = tryAdd(feedToAdd);
            if (!added) {
                log.info("failed to add feed " + feedToAdd + " to dataflows " + dataFlows + " - will try again later");
                feedToAddList.add(feedToAdd);
            } else {
                feeds = ArrayUtil.add(feeds, feedToAdd);

                for (DataFlow dataFlow : dataFlows) {
                    log.info(() -> "adding eventFeed " + feedToAdd + " to dataflow " + dataFlow);
                    feedToAdd.registerSubscriber(dataFlow);
                    dataFlow.registerService(feedToAdd, NamedFeed.class, feedToAdd.roleName());
                }
            }
        }

        EventFeedAgent<?> feedToRemove = feedToRemoveList.poll();
        if (feedToRemove != null) {
            log.info(() -> "remove feed " + feedToRemove);
            feeds = ArrayUtil.remove(feeds, feedToRemove);

            for (DataFlow dataFlow : dataFlows) {
                log.info(() -> "removing eventFeed " + feedToRemove + " to dataflow " + dataFlow);
                dataFlow.deRegisterService(feedToRemove, NamedFeed.class, feedToAdd.roleName());
                feedToRemove.removeAllSubscriptions(dataFlow);
            }
        }
    }
}