// BROWSER-BUNDLE OVERRIDE
//
// Re-pack of fluxtion-builder-api/src/main/java/com/telamin/fluxtion/
// builder/flowfunction/FlowDataSupplier.java with the build-config consumer
// kept lambda-free. CheerpJ Java 8 mode fails to re-resolve invokedynamic call
// sites in JAR-shipped classes on the second hit per JVM.
//
// Keep in sync with the upstream file.

package com.telamin.fluxtion.builder.flowfunction;

import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import com.telamin.fluxtion.builder.generation.context.GenerationContextHolder;
import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.audit.EventLogControlEvent;
import com.telamin.fluxtion.runtime.flowfunction.FlowSupplier;

public interface FlowDataSupplier<T extends FlowSupplier<?>> {
    T flowSupplier();

    default DataFlow build() {
        GenerationContext context = GenerationContextHolder.ensureInlineContext();
        try {
            return DataFlowBuildService.load(context).build(context);
        } finally {
            GenerationContextHolder.clearInlineContext();
        }
    }

    default DataFlow build(EventLogControlEvent.LogLevel logLevel) {
        GenerationContext context = GenerationContextHolder.ensureInlineContext();
        try {
            return DataFlowBuildService.load(context).build(context, logLevel);
        } finally {
            GenerationContextHolder.clearInlineContext();
        }
    }
}
