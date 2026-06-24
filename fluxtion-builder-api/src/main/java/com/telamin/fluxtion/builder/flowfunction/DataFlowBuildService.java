package com.telamin.fluxtion.builder.flowfunction;

import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.audit.EventLogControlEvent;

import java.util.ServiceLoader;

/**
 * Terminal bridge from the authoring DSL to a build-engine provider.
 */
public interface DataFlowBuildService {

    DataFlow build(GenerationContext context);

    DataFlow build(
            GenerationContext context,
            EventLogControlEvent.LogLevel auditLogLevel);

    static DataFlowBuildService load(GenerationContext context) {
        ClassLoader classLoader = context == null ? null : context.getClassLoader();
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        if (classLoader == null) {
            classLoader = DataFlowBuildService.class.getClassLoader();
        }

        for (DataFlowBuildService service
                : ServiceLoader.load(DataFlowBuildService.class, classLoader)) {
            return service;
        }
        throw new IllegalStateException(
                "DataFlowBuilder.build() requires a Fluxtion build-engine provider. "
                        + "Add com.telamin.fluxtion:fluxtion-builder for local builds, "
                        + "preferably via com.telamin.fluxtion:fluxtion-bom, or run a "
                        + "pre-generated processor with fluxtion-runtime only.");
    }
}
