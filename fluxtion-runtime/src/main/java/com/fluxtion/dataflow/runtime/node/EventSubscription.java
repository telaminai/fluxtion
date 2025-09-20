/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.node;

import com.fluxtion.dataflow.runtime.event.Event;
import lombok.Getter;

import java.util.Objects;

public class EventSubscription<T> implements Event {
    @Getter
    private final String feedName;
    private final int filterId;
    private final String filterString;
    private final Class<T> eventClass;
    private String toString;
    protected long eventTime;

    public EventSubscription(int filterId, String filterString, Class<T> eventClass) {
        this.filterId = filterId;
        this.filterString = filterString;
        this.eventClass = eventClass;
        this.eventTime = System.currentTimeMillis();
        this.feedName = "";
        toString = "EventSubscription{" +
                "feedName=*" +
                ",  eventClass=" + eventClass +
                (filterId == Integer.MAX_VALUE ? "" : ", filterId=" + filterId) +
                (filterString.isEmpty() ? "" : ", filterString=" + filterString) +
                '}';
    }

    public EventSubscription(String feedName, int filterId, String filterString, Class<T> eventClass) {
        this.feedName = feedName;
        this.filterId = filterId;
        this.filterString = filterString;
        this.eventClass = eventClass;
        this.eventTime = System.currentTimeMillis();
        toString = "EventSubscription{" +
                "feedName=" + feedName +
                ", eventClass=" + eventClass +
                (filterId == Integer.MAX_VALUE ? "" : ", filterId=" + filterId) +
                (filterString.isEmpty() ? "" : ", filterString=" + filterString) +
                '}';
    }

    public int filterId() {
        return filterId;
    }

    public String filterString() {
        return filterString;
    }

    public Class<T> eventClass() {
        return eventClass;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventSubscription<?> that = (EventSubscription<?>) o;
        return feedName.equals(that.feedName) && filterId == that.filterId && filterString.equals(that.filterString) && Objects.equals(eventClass, that.eventClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filterId, filterString, eventClass, feedName);
    }

    @Override
    public String toString() {
        return toString;
    }
}
