/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.node;

import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.event.NamedFeedEvent;

import java.util.Objects;

public class NamedFeedTopicFilteredEventHandlerNode<T>
        extends NamedFeedEventHandlerNode<T> {


    private final String topic;

    public NamedFeedTopicFilteredEventHandlerNode(
            @AssignToField("feedName") String feedName,
            @AssignToField("topic") String topic
    ) {
        super(feedName, "eventFeedHandler_" + feedName + "_" + topic);
        Objects.requireNonNull(topic, "topic cannot be null");
        this.topic = topic;
    }


    @Override
    public <E extends NamedFeedEvent<?>> boolean onEvent(E e) {
        if (e.topic() != null && topic.equals(e.topic())) {
            feedEvent = (NamedFeedEvent<T>) e;
            return true;
        }
        return false;
    }
}
