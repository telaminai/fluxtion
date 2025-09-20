/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.node;

import com.telamin.fluxtion.runtime.annotations.Initialise;
import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.annotations.runtime.ServiceRegistered;
import com.telamin.fluxtion.runtime.event.NamedFeedEvent;
import com.telamin.fluxtion.runtime.input.NamedFeed;
import com.telamin.fluxtion.runtime.partition.LambdaReflection;
import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("all")
public class NamedFeedTableNode<K, V> extends BaseNode implements TableNode<K, V> {

    private final String feedName;
    private final LambdaReflection.SerializableFunction keyMethodReference;
    private final String topicName;
    private transient final Map tableMap = new HashMap();
    private transient final Map tableMapReadonly = Collections.unmodifiableMap(tableMap);
    private long lastSequenceNumber;
    @Getter
    private NamedFeedEvent lastFeedEvent;

    public NamedFeedTableNode(String feedName, String keyFunction) {
        this(feedName, null, keyFunction);
    }

    @SneakyThrows
    public NamedFeedTableNode(String feedName, String topicName, String keyFunction) {
        Objects.requireNonNull(feedName, "feedName cannot be null");
        Objects.requireNonNull(keyFunction, "keyFunction cannot be null");
        this.feedName = feedName;
        this.topicName = topicName;
        String[] functionString = keyFunction.split("::");
        Class<?> clazz = Class.forName(functionString[0]);
        Method keyMethod = clazz.getMethod(functionString[1]);
        keyMethodReference = LambdaReflection.method2Function(keyMethod);
        Objects.requireNonNull(keyMethodReference, "keyMethodReference cannot be null");
    }

    public <T, R> NamedFeedTableNode(@AssignToField("feedName") String feedName,
                                     @AssignToField("keyMethodReference") LambdaReflection.SerializableFunction<T, R> keyMethodReference) {
        this.feedName = feedName;
        this.keyMethodReference = keyMethodReference;
        this.topicName = null;
    }

    public <T, R> NamedFeedTableNode(@AssignToField("feedName") String feedName,
                                     @AssignToField("topicName") String topicName,
                                     @AssignToField("keyMethodReference") LambdaReflection.SerializableFunction<T, R> keyMethodReference) {
        this.feedName = feedName;
        this.topicName = topicName;
        this.keyMethodReference = keyMethodReference;
    }

    @Initialise
    public void initialise() {
        lastSequenceNumber = -1;
        auditLog.info("subscribe", feedName);
        getContext().subscribeToNamedFeed(feedName);
    }

    @ServiceRegistered
    public void serviceRegistered(NamedFeed feed, String feedName) {
        if (feedName != null && feedName.equals(this.feedName)) {
            auditLog.info("requestSnapshot", feedName)
                    .info("eventLogSize", feed.eventLog().length);
            NamedFeedEvent<V>[] eventLog = feed.eventLog();
            for (NamedFeedEvent<V> namedFeedEvent : eventLog) {
                tableUpdate(namedFeedEvent);
            }
        } else {
            auditLog.info("ignoreFeedSnapshot", feedName);
        }
    }

    @SneakyThrows
    @OnEventHandler(filterVariable = "feedName")
    public boolean tableUpdate(NamedFeedEvent feedEvent) {
        if (feedEvent.sequenceNumber() > lastSequenceNumber & (topicName == null || topicName.equals(feedEvent.topic()))) {
            this.lastFeedEvent = feedEvent;
            Object dataItem = feedEvent.data();
            lastSequenceNumber = feedEvent.sequenceNumber();
            Object key = keyMethodReference.apply(dataItem);
            auditLog.debug("received", feedEvent);
            if (feedEvent.delete()) {
                auditLog.debug("deletedKey", key);
                tableMap.remove(key);
            } else {
                auditLog.debug("putKey", key);
                tableMap.put(key, dataItem);
            }
            return true;
        }
        return false;
    }

    @Override
    public Map<K, V> getTableMap() {
        return tableMapReadonly;
    }
}
