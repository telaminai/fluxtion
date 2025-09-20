/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.flowfunction;

import com.fluxtion.dataflow.builder.generation.context.GenerationContext;
import com.fluxtion.dataflow.builder.generation.target.InMemoryEventProcessorBuilder;
import com.fluxtion.dataflow.runtime.CloneableDataFlow;
import com.fluxtion.dataflow.runtime.DataFlow;
import com.fluxtion.dataflow.runtime.audit.EventLogControlEvent;
import com.fluxtion.dataflow.runtime.flowfunction.FlowSupplier;

import java.util.List;
import java.util.Map;

public interface FlowDataSupplier<T extends FlowSupplier<?>> {
    T flowSupplier();

    /**
     * Build a {@link DataFlow} without any audi logging added
     *
     * @return the {@link DataFlow} to use in an application
     */
    default DataFlow build() {
        List<Object> nodeList = GenerationContext.SINGLETON.getNodeList();
        Map<Object, String> publisNodeMap = GenerationContext.SINGLETON.getPublicNodes();
        CloneableDataFlow<?> eventProcessor = InMemoryEventProcessorBuilder.interpreted(
                c -> {
                    for (Object node : nodeList) {
                        c.addNode(node);
                    }

                    publisNodeMap.forEach(c::addPublicNode);
                }, false);
        eventProcessor.init();
        return eventProcessor;
    }

    /**
     * Build a {@link DataFlow} adding an audit logger with the specified {@link EventLogControlEvent.LogLevel}
     *
     * @param logLevel the audit log level to trace at. Null value mean no tracing but audit logger is added
     * @return the {@link DataFlow} to use in an application
     */
    default DataFlow build(EventLogControlEvent.LogLevel logLevel) {
        List<Object> nodeList = GenerationContext.SINGLETON.getNodeList();
        Map<Object, String> publisNodeMap = GenerationContext.SINGLETON.getPublicNodes();
        CloneableDataFlow<?> eventProcessor = InMemoryEventProcessorBuilder.interpreted(
                c -> {
                    for (Object node : nodeList) {
                        c.addNode(node);
                    }

                    publisNodeMap.forEach(c::addPublicNode);
                    c.addEventAudit(logLevel);
                }, false);
        eventProcessor.init();
        return eventProcessor;
    }
}
