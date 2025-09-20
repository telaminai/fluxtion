/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.runtime.event;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Default implementation of {@link Event} that can be extended to provide
 * meta-data to user events.
 *
 * @author Greg Higgins
 */
@EqualsAndHashCode
@ToString
public abstract class DefaultEvent implements Event {

    public static final int NO_INT_FILTER = Integer.MAX_VALUE;
    public static final String NO_STRING_FILTER = "";

    protected int filterId;
    protected String filterString;
    protected long eventTime;

    public DefaultEvent() {
        this(NO_STRING_FILTER);
    }

    public DefaultEvent(String filterId) {
        this(NO_INT_FILTER, filterId);
    }

    public DefaultEvent(int filterId) {
        this(filterId, NO_STRING_FILTER);
    }

    public DefaultEvent(int filterId, String filterString) {
        this.filterId = filterId;
        this.filterString = filterString;
        this.eventTime = System.currentTimeMillis();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public final int filterId() {
        return filterId;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public final String filterString() {
        return filterString;
    }

    public final CharSequence filterCharSequence() {
        return filterString;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public long getEventTime() {
        return eventTime;
    }

    /**
     * Override the default value for event creation time. The default value is
     * set with {@link System#currentTimeMillis()} during construction. The
     * value must be greater than 0, otherwise the value is ignored
     *
     * @param eventTime
     */
    public void setEventTime(long eventTime) {
        if (eventTime > 0) {
            this.eventTime = eventTime;
        }
    }

}
