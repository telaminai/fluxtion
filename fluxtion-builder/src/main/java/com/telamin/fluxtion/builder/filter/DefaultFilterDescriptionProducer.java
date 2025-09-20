/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.filter;

import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import com.telamin.fluxtion.runtime.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.ServiceLoader;

/**
 * @author Greg Higgins
 */
public class DefaultFilterDescriptionProducer implements FilterDescriptionProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFilterDescriptionProducer.class);
    private ArrayList<FilterDescriptionProducer> namingStrategies;

    public DefaultFilterDescriptionProducer() {
        loadServices();
    }

    public final void loadServices() {
        LOGGER.debug("DefaultFilterDescriptionProducer (re)loading strategies");
        ServiceLoader<FilterDescriptionProducer> loadServices;
        namingStrategies = new ArrayList<>();
        if (GenerationContext.SINGLETON != null && GenerationContext.SINGLETON.getClassLoader() != null) {
            LOGGER.debug("using custom class loader to search for NodeNameProducer");
            loadServices = ServiceLoader.load(FilterDescriptionProducer.class, GenerationContext.SINGLETON.getClassLoader());
        } else {
            loadServices = ServiceLoader.load(FilterDescriptionProducer.class);
        }
        loadServices.forEach(namingStrategies::add);
//        Collections.sort(namingStrategies);
        LOGGER.debug("sorted FilterDescriptionProducer strategies : {}", namingStrategies);
    }

    @Override
    public FilterDescription getFilterDescription(Class<? extends Event> event, int filterId) {
        final FilterDescription filterDescription = FilterDescriptionProducer.super.getFilterDescription(event, filterId);
        filterDescription.comment = "Event Class:[" + event.getCanonicalName() + "]"
                + " filterId:[" + filterId + "]";
        for (FilterDescriptionProducer namingStrategy : namingStrategies) {
            String commnent = namingStrategy.getFilterDescription(event, filterId).comment;
            if (commnent != null) {
                filterDescription.comment = commnent;
                break;
            }
        }
        return filterDescription;
    }

    @Override
    public FilterDescription getFilterDescription(Class<? extends Event> event, String filterId) {
        final FilterDescription filterDescription = FilterDescriptionProducer.super.getFilterDescription(event, filterId);
        filterDescription.comment = "Event Class:[" + event.getCanonicalName() + "]"
                + " filterString:[" + filterId + "]";
        for (FilterDescriptionProducer namingStrategy : namingStrategies) {
            String commnent = namingStrategy.getFilterDescription(event, filterId).comment;
            if (commnent != null) {
                filterDescription.comment = commnent;
                break;
            }
        }
        return filterDescription;
    }

}
