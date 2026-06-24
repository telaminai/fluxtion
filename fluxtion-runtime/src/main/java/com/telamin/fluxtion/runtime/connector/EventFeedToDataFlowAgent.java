/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.connector;

import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.annotations.feature.Experimental;
import com.telamin.fluxtion.runtime.eventfeed.EventFeedAgent;
import com.telamin.fluxtion.runtime.input.NamedFeed;
import com.telamin.fluxtion.runtime.lifecycle.Lifecycle;
import com.telamin.fluxtion.runtime.output.MessageSink;
import com.telamin.fluxtion.runtime.output.SinkRegistration;
import com.telamin.fluxtion.runtime.service.Service;
import lombok.extern.java.Log;
import org.agrona.collections.ArrayUtil;
import org.agrona.concurrent.OneToOneConcurrentArrayQueue;

import java.util.HashMap;
import java.util.Map;
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
    private final OneToOneConcurrentArrayQueue<Service<?>> servicesToAdd;
    private final OneToOneConcurrentArrayQueue<Service<?>> servicesToRemove;
    private DataFlow[] dataFlows;
    // Services registered with every hosted DataFlow (kept in sync as DataFlows come/go).
    // Includes sinks: addSink also registers the consumer so an imperative node's
    // @ServiceRegistered MessageSink is injected, not just the DSL .sink() terminal.
    private Service<?>[] services;
    private final Map<String, Service<?>> sinkServices = new HashMap<>();
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
        servicesToAdd = new OneToOneConcurrentArrayQueue<>(128);
        servicesToRemove = new OneToOneConcurrentArrayQueue<>(128);
        dataFlows = new DataFlow[0];
        services = new Service[0];
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

    public <S, T extends S> void registerService(T service, Class<S> serviceClass, String serviceName) {
        servicesToAdd.add(new Service<>(service, serviceClass, serviceName));
    }

    public <S, T extends S> void deRegisterService(T service, Class<S> serviceClass) {
        servicesToRemove.add(new Service<>(service, serviceClass));
    }

    /** The service a sink is registered as: MessageSink when it is one (so an imperative
     *  {@code @ServiceRegistered MessageSink} matches), otherwise its concrete class. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Service<?> sinkService(Consumer<?> consumer, String id) {
        Class<?> type = (consumer instanceof MessageSink) ? MessageSink.class : consumer.getClass();
        return new Service(consumer, type, id);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerOn(DataFlow dataFlow, Service<?> svc) {
        dataFlow.registerService(svc.instance(), (Class) svc.serviceClass(), svc.serviceName());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void deRegisterOn(DataFlow dataFlow, Service<?> svc) {
        dataFlow.deRegisterService(svc.instance(), (Class) svc.serviceClass());
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
            for (Service<?> svc : services) {
                registerOn(dataFlow, svc); // re-register existing sinks/services with the new flow
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
            Consumer<?> consumer = sink.getConsumer();
            if (consumer instanceof Lifecycle) {
                Lifecycle lifecycle = (Lifecycle) consumer;
                lifecycle.init();
                lifecycle.start();
            }
            // Register the sink as a service too, so an imperative node's @ServiceRegistered
            // MessageSink is injected (not only the DSL .sink() terminal via dataFlow.addSink).
            Service<?> svc = sinkService(consumer, sink.filterString());
            services = ArrayUtil.add(services, svc);
            sinkServices.put(sink.filterString(), svc);
            for (DataFlow dataFlow : dataFlows) {
                log.info(() -> "adding sink " + sink + " to dataflow " + dataFlow);
                dataFlow.addSink(sink.filterString(), consumer);
                registerOn(dataFlow, svc);
            }
        });

        sinksToRemove.drain(sinkId -> {
            for (DataFlow dataFlow : dataFlows) {
                log.info(() -> "removing sink " + sinkId + " to dataflow " + dataFlow);
                dataFlow.removeSink(sinkId);
            }
            Service<?> svc = sinkServices.remove(sinkId);
            if (svc != null) {
                services = ArrayUtil.remove(services, svc);
                for (DataFlow dataFlow : dataFlows) {
                    deRegisterOn(dataFlow, svc);
                }
            }
        });

        servicesToAdd.drain(svc -> {
            services = ArrayUtil.add(services, svc);
            for (DataFlow dataFlow : dataFlows) {
                registerOn(dataFlow, svc);
            }
        });

        servicesToRemove.drain(svc -> {
            for (DataFlow dataFlow : dataFlows) {
                deRegisterOn(dataFlow, svc);
            }
            for (Service<?> registered : services) {
                if (registered.instance() == svc.instance()
                        && registered.serviceClass() == svc.serviceClass()) {
                    services = ArrayUtil.remove(services, registered);
                    break;
                }
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