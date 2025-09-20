/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.node;

import com.fluxtion.dataflow.runtime.DataFlow;
import com.fluxtion.dataflow.runtime.annotations.OnTrigger;
import com.fluxtion.dataflow.runtime.event.Event;

/**
 * Acts as the root of an execution path in a {@link DataFlow}. A
 * user implements this class and registers it with the static event compiler,
 * to generate a DataFlow. Events will be routed to an instance of
 * this class by the generated DataFlow at runtime.
 *
 * <pre>
 * <h2>Filtering</h2>
 * An EventHandler can optionally provide a filter value to filter the
 * events that are accepted for processing. Usually the match is based solely
 * on event type to determine if instance of a FilteredEventHandler is on the
 * execution path for an event, filtering can further refine the match.
 * <p>
 *
 * An {@link Event} can optionally specify a filter value as an int {@link Event#filterId()
 * } or as a String {@link Event#filterString() . The SEP will compare the filter
 * values in the {@link Event} and the handler and propagate the Event conditional upon the a match.
 * .<p>
 *
 * Default values for filters indicate only match on type, no filters are applied:
 * <ul>
 * <li>int filter : Integer.MAX_VALUE = no filtering</li>
 * <li>String filter : null or "" = no filtering</li>
 * </ul>
 * </pre>
 * <p>
 * Child instances that refer to this instance receive update callbacks by invoking marking a method with an {@link OnTrigger }
 * annotation or implementing the {@link TriggeredNode} interface
 *
 * @param <T> The type of event processed by this handler
 * @author Greg Higgins
 */
public interface EventHandlerNode<T> {

    default int filterId() {
        return Event.NO_INT_FILTER;
    }

    default String filterString() {
        return Event.NO_STRING_FILTER;
    }

    /**
     * Called when a new event e is ready to be processed. Return flag indicates if a notification should be broadcast
     * and the onEvent methods of dependencies should be invoked
     *
     * @param e the {@link Event Event} to process.
     * @return event propagtion flag
     */
    <E extends T> boolean onEvent(E e);

    /**
     * called when all nodes that depend upon this EventHadler have successfully
     * completed their processing.
     */
    default void afterEvent() {
    }

    /**
     * The class of the Event processed by this handler, overrides the generic type parameter class. If null then the class
     * of the generic type T is used to determine the event type to be processed.
     *
     * @return Class of {@link Event Event} to process
     */
    default Class<? extends T> eventClass() {
        return null;
    }
}
