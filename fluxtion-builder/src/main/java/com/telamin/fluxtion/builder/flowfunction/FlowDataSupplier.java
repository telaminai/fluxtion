/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.flowfunction;

import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import com.telamin.fluxtion.builder.generation.target.InMemoryEventProcessorBuilder;
import com.telamin.fluxtion.runtime.CloneableDataFlow;
import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.audit.EventLogControlEvent;
import com.telamin.fluxtion.runtime.flowfunction.FlowSupplier;

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
