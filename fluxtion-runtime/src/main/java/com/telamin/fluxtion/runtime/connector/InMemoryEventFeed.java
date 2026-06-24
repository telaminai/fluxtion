/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.connector;

import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.eventfeed.BaseEventFeed;
import com.telamin.fluxtion.runtime.eventfeed.ReadStrategy;
import com.telamin.fluxtion.runtime.node.EventSubscription;
import lombok.extern.java.Log;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;

@Log
public class InMemoryEventFeed<T> extends BaseEventFeed<T> {

    private final ConcurrentLinkedDeque<T> messages = new ConcurrentLinkedDeque<>();

    public InMemoryEventFeed(String feedName) {
        super(feedName);
    }

    public InMemoryEventFeed(String feedName, boolean cacheEventLog, boolean broadcast) {
        super(feedName, cacheEventLog,  ReadStrategy.EARLIEST, broadcast);
    }

    public void publish(T event) {
        messages.add(event);
    }

    @Override
    public int doWork() throws Exception {
        T event = messages.poll();
        int count = 0;
        while (event != null) {
            if(log.isLoggable(Level.FINE)){
                log.finer(String.format("Processing event %s", event));
            }
            super.publish(event);
            event = messages.poll();
            count = 1;
        }
        return count;
    }

    @Override
    protected boolean validSubscription(DataFlow subscriber, EventSubscription<?> subscriptionId) {
        boolean validSubscription = subscriptionId.getFeedName().equals(getFeedName());
        log.fine(() -> "feedName:" + getFeedName() + " subscription:" + subscriptionId.getFeedName() + " validSubscription:" + validSubscription);
        return validSubscription;
    }
}
