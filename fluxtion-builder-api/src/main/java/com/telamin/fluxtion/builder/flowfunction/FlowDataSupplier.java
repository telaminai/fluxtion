/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.builder.flowfunction;

import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import com.telamin.fluxtion.builder.generation.context.GenerationContextHolder;
import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.audit.EventLogControlEvent;
import com.telamin.fluxtion.runtime.flowfunction.FlowSupplier;

public interface FlowDataSupplier<T extends FlowSupplier<?>> {
    T flowSupplier();

    /**
     * Build a {@link DataFlow} without any audi logging added
     *
     * @return the {@link DataFlow} to use in an application
     */
    default DataFlow build() {
        GenerationContext context = GenerationContextHolder.ensureInlineContext();
        try {
            return DataFlowBuildService.load(context).build(context);
        } finally {
            GenerationContextHolder.clearInlineContext();
        }
    }

    /**
     * Build a {@link DataFlow} adding an audit logger with the specified {@link EventLogControlEvent.LogLevel}
     *
     * @param logLevel the audit log level to trace at. Null value mean no tracing but audit logger is added
     * @return the {@link DataFlow} to use in an application
     */
    default DataFlow build(EventLogControlEvent.LogLevel logLevel) {
        GenerationContext context = GenerationContextHolder.ensureInlineContext();
        try {
            return DataFlowBuildService.load(context).build(context, logLevel);
        } finally {
            GenerationContextHolder.clearInlineContext();
        }
    }
}
