/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.filter;

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
