/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */
package com.fluxtion.dataflow.builder.filter;

import com.fluxtion.dataflow.runtime.event.Event;

import java.util.ServiceLoader;

/**
 * Produces {@link FilterDescription} instances that act as a extension points
 * for control of filter comments and variable names in the generated SEP.
 *
 * <h2>Registering factories</h2>
 * Fluxtion employs the {@link ServiceLoader} pattern to register user
 * implemented FilterDescriptionProducer's. Please read the java documentation
 * describing the meta-data a node implementor must provide to register a
 * node using the {@link ServiceLoader} pattern.
 *
 * @author Greg Higgins
 */
public interface FilterDescriptionProducer {

    default FilterDescription getFilterDescription(Class<? extends Event> event, int filterId) {
        FilterDescription filter = new FilterDescription(event, filterId);
        filter.comment = null;
        filter.variableName = null;
        return filter;
    }

    default FilterDescription getFilterDescription(Class<? extends Event> event, String filterString) {
        FilterDescription filter = new FilterDescription(event, filterString);
        filter.comment = null;
        filter.variableName = null;
        return filter;
    }

}
