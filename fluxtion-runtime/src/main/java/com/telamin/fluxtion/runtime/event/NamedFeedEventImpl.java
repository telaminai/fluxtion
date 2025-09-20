/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.event;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true, fluent = true)
public class NamedFeedEventImpl<T> extends DefaultEvent implements NamedFeedEvent<T> {

    private String topic;
    private T data;
    private boolean delete;
    private long sequenceNumber;

    public NamedFeedEventImpl(String eventFeedName) {
        this(eventFeedName, null, null);
    }

    public NamedFeedEventImpl(String eventFeedName, T data) {
        this(eventFeedName, null, data);
    }

    public NamedFeedEventImpl(String filterId, long sequenceNumber, T data) {
        this(filterId, null, data);
    }

    public NamedFeedEventImpl(String eventFeedName, String topic, T data) {
        super(eventFeedName);
        sequenceNumber++;
        this.topic = topic;
        this.data = data;
    }

    public NamedFeedEventImpl(String eventFeedName, String topic) {
        this(eventFeedName, topic, 0, null);
    }

    public NamedFeedEventImpl(String filterId, String topic, long sequenceNumber, T data) {
        super(filterId);
        this.sequenceNumber = sequenceNumber;
        this.topic = topic;
        this.data = data;
    }

    public NamedFeedEventImpl<T> copyFrom(NamedFeedEventImpl<T> other) {
        topic(other.topic);
        data(other.data);
        delete(other.delete);
        sequenceNumber(other.sequenceNumber);
        filterId = other.filterId;
        setEventFeedName(eventFeedName());
        setEventTime(getEventTime());
        return this;
    }

    public NamedFeedEventImpl<T> clone() {
        NamedFeedEventImpl<T> namedFeedEvent = new NamedFeedEventImpl<>(eventFeedName(), topic(), sequenceNumber(), data());
        namedFeedEvent.copyFrom(this);
        return namedFeedEvent;
    }

    public void setEventFeedName(String eventFeedName) {
        this.filterString = eventFeedName;
    }

    @Override
    public String eventFeedName() {
        return filterString;
    }

    @Override
    public String toString() {
        return "NamedFeedEvent{" +
                "eventFeed='" + filterString + '\'' +
                ", topic='" + topic + '\'' +
                ", sequenceNumber='" + sequenceNumber + '\'' +
                ", delete='" + delete + '\'' +
                ", data=" + data +
                ", eventTime=" + eventTime +
                '}';
    }
}
