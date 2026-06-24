/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.model;

import com.telamin.fluxtion.runtime.event.Event;

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

    /**
     * A minimal default instance that creates FilterDescriptions with no comment or variable name.
     * Used by server-side model generation where no ServiceLoader-based strategies are available.
     */
    FilterDescriptionProducer DEFAULT_INSTANCE = new FilterDescriptionProducer() {};

    default FilterDescription getFilterDescription(Class<? extends Event> event, int filterId) {
        return getFilterDescription(event == null ? null : event.getName(), filterId);
    }

    default FilterDescription getFilterDescription(Class<? extends Event> event, String filterString) {
        return getFilterDescription(event == null ? null : event.getName(), filterString);
    }

    default FilterDescription getFilterDescription(String eventClassName, int filterId) {
        FilterDescription filter = new FilterDescription(eventClassName, filterId);
        filter.setComment(null);
        filter.setVariableName(null);
        return filter;
    }

    default FilterDescription getFilterDescription(String eventClassName, String filterString) {
        FilterDescription filter = new FilterDescription(eventClassName, filterString);
        filter.setComment(null);
        filter.setVariableName(null);
        return filter;
    }

}
