/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.connector;

import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.annotations.feature.Experimental;
import com.telamin.fluxtion.runtime.eventfeed.EventFeedAgent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.java.Log;
import org.agrona.concurrent.*;
import org.agrona.concurrent.status.AtomicCounter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Applications can use this class to broke messages between {@link EventFeedAgent}'s and {@link DataFlow}'s
 */
@Experimental
@Log
public class DataFlowConnector {

    private final AtomicCounter errorCounter = new AtomicCounter(new UnsafeBuffer(new byte[4096]), 0);
    @Getter(AccessLevel.PROTECTED)
    private final EventFeedToDataFlowAgent agent = new EventFeedToDataFlowAgent("dataFlowIOAgentRunner");
    private final AgentRunner agentRunner;
    private final Consumer<Throwable> errorHandler;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public DataFlowConnector() {
        this(new BackoffIdleStrategy(10, 10, 1_000_000, 1_000_000_000));
    }

    public DataFlowConnector(IdleStrategy idleStrategy) {
        this(idleStrategy, e -> e.printStackTrace(System.err));
    }

    public DataFlowConnector(IdleStrategy idleStrategy, Consumer<Throwable> errorHandler) {
        this.agentRunner = new AgentRunner(
                idleStrategy,
                this::errorHandler,
                errorCounter,
                agent
        );
        this.errorHandler = errorHandler;
    }

    public DataFlowConnector addFeed(EventFeedAgent<?> feed) {
        agent.addFeed(feed);
        return this;
    }

    public DataFlowConnector removeFeed(EventFeedAgent<?> feed) {
        agent.removeFeed(feed);
        return this;
    }

    public DataFlowConnector addDataFlow(DataFlow dataFlow) {
        agent.addDataFlow(dataFlow);
        return this;
    }

    public DataFlowConnector removeDataFlow(DataFlow dataFlow) {
        agent.removeDataFlow(dataFlow);
        return this;
    }

    public <T> DataFlowConnector addSink(String id, Consumer<T> sink) {
        agent.addSink(id, sink);
        return this;
    }

    public <T> DataFlowConnector removeSink(String id) {
        agent.removeSink(id);
        return this;
    }

    /** Register a service with every hosted DataFlow — injected into matching
     *  {@code @ServiceRegistered} node methods (a cache, clock, sink, etc.). */
    public <S, T extends S> DataFlowConnector registerService(T service, Class<S> serviceClass, String serviceName) {
        agent.registerService(service, serviceClass, serviceName);
        return this;
    }

    public <S, T extends S> DataFlowConnector deRegisterService(T service, Class<S> serviceClass) {
        agent.deRegisterService(service, serviceClass);
        return this;
    }

    @SuppressWarnings("BusyWait")
    public synchronized DataFlowConnector start() {
        if (!started.get()) {
            log.info("Starting DataFlowRunner");
            AgentRunner.startOnThread(agentRunner);
        } else {
            log.fine("DataFlowRunner already started");
        }
        started.set(true);
        // Wait for the agent thread to reach ACTIVE. Sleep between checks
        // rather than busy-spin: yields the calling thread to the
        // scheduler so the agent thread actually gets CPU to flip the
        // status. Critical under cooperative-scheduling environments
        // (e.g. CheerpJ in the browser) where a tight spin on the
        // caller starves the agent and locks the host's event loop.
        while (agent.status() != WorkDynamicCompositeAgent.Status.ACTIVE) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return this;
    }

    public DataFlowConnector stop() {
        if (!agentRunner.isClosed()) {
            agentRunner.close();
        }
        return this;
    }

    private void errorHandler(Throwable throwable) {
        log.severe(throwable::getMessage);
        errorHandler.accept(throwable);
    }
}