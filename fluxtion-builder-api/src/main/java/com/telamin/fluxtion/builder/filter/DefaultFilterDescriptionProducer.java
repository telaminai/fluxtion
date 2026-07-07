/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.builder.filter;
import com.telamin.fluxtion.builder.meta.spi.FilterDescriptionProducer;
import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import com.telamin.fluxtion.builder.generation.context.GenerationContextHolder;
import com.telamin.fluxtion.runtime.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.ServiceLoader;

/**
 * @author Greg Higgins
 */
public class DefaultFilterDescriptionProducer implements com.telamin.fluxtion.builder.meta.spi.FilterDescriptionProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFilterDescriptionProducer.class);
    private ArrayList<com.telamin.fluxtion.builder.meta.spi.FilterDescriptionProducer> namingStrategies;

    public DefaultFilterDescriptionProducer() {
        loadServices();
    }

    public final void loadServices() {
        LOGGER.debug("DefaultFilterDescriptionProducer (re)loading strategies");
        ServiceLoader<FilterDescriptionProducer> loadServices;
        namingStrategies = new ArrayList<>();
        if (GenerationContextHolder.currentOrNull() != null && GenerationContextHolder.currentOrNull().getClassLoader() != null) {
            LOGGER.debug("using custom class loader to search for NodeNameProducer");
            loadServices = ServiceLoader.load(com.telamin.fluxtion.builder.meta.spi.FilterDescriptionProducer.class, GenerationContextHolder.currentOrNull().getClassLoader());
        } else {
            loadServices = ServiceLoader.load(com.telamin.fluxtion.builder.meta.spi.FilterDescriptionProducer.class);
        }
        loadServices.forEach(namingStrategies::add);
//        Collections.sort(namingStrategies);
        LOGGER.debug("sorted FilterDescriptionProducer strategies : {}", namingStrategies);
    }

    @Override
    public com.telamin.fluxtion.builder.meta.model.FilterDescription getFilterDescription(Class<? extends Event> event, int filterId) {
        final com.telamin.fluxtion.builder.meta.model.FilterDescription filterDescription = com.telamin.fluxtion.builder.meta.spi.FilterDescriptionProducer.super.getFilterDescription(event, filterId);
        filterDescription.setComment("Event Class:[" + event.getCanonicalName() + "]"
                + " filterId:[" + filterId + "]");
        for (com.telamin.fluxtion.builder.meta.spi.FilterDescriptionProducer namingStrategy : namingStrategies) {
            String commnent = namingStrategy.getFilterDescription(event, filterId).getComment();
            if (commnent != null) {
                filterDescription.setComment(commnent);
                break;
            }
        }
        return filterDescription;
    }

    @Override
    public com.telamin.fluxtion.builder.meta.model.FilterDescription getFilterDescription(Class<? extends Event> event, String filterId) {
        final com.telamin.fluxtion.builder.meta.model.FilterDescription filterDescription = com.telamin.fluxtion.builder.meta.spi.FilterDescriptionProducer.super.getFilterDescription(event, filterId);
        filterDescription.setComment("Event Class:[" + event.getCanonicalName() + "]"
                + " filterString:[" + filterId + "]");
        for (com.telamin.fluxtion.builder.meta.spi.FilterDescriptionProducer namingStrategy : namingStrategies) {
            String commnent = namingStrategy.getFilterDescription(event, filterId).getComment();
            if (commnent != null) {
                filterDescription.setComment(commnent);
                break;
            }
        }
        return filterDescription;
    }

}
