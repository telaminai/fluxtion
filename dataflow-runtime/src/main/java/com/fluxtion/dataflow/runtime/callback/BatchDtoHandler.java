/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.callback;

import com.fluxtion.dataflow.runtime.annotations.OnEventHandler;
import com.fluxtion.dataflow.runtime.node.BaseNode;

/**
 * The {@code BatchDtoHandler} class extends {@code BaseNode} and provides a mechanism
 * for processing batch events. It is designed to handle incoming {@code BatchDto}
 * objects and dispatch each item within the batch for further processing by the parent
 * {@code DataFlow} context.
 *
 * <h2>Processing Flow</h2>
 * When an event of type {@code BatchDto} is received:
 * - Log the received batch event using {@code auditLog}.
 * - Iterate over the batch data contained within the {@code BatchDto}.
 * - Log each individual event in the batch.
 * - Dispatch each event to the parent {@code DataFlow} context for additional
 *   processing, using the {@code onEvent} method of the parent context.
 *
 * <h2>Event Handling</h2>
 * The processing method {@code processBatch(BatchDto batchEvent)} is annotated with
 * {@code @OnEventHandler}, which designates it as an entry point for event handling.
 * This method overrides the default boolean processing behavior to return "false",
 * indicating no change to the event path based on processing logic.
 *
 * <h2>Dependencies</h2>
 * - The {@code BatchDtoHandler} depends on the {@code BatchDto} class for batch events.
 * - It logs audit information using {@code auditLog}.
 * - It uses the parent {@code DataFlowContext} for dispatching events.
 *
 * <h2>Threading</h2>
 * No specific thread safety guarantees are provided. Ensure external synchronization
 * if used in a multi-threaded environment.
 *
 * <h2>Customization</h2>
 * Subclasses of {@code BatchDtoHandler} can extend or override the functionality
 * defined herein to meet specific requirements for batch processing.
 *
 * <h2>Usage</h2>
 * The {@code processBatch} method acts as a processing entry point within a Fluxtion
 * event-driven execution graph. Ensure the handler is appropriately integrated through
 * the Fluxtion builder API to support event dispatch and processing.
 */
public class BatchDtoHandler extends BaseNode {
    
    @OnEventHandler
    public boolean processBatch(BatchDto batchEvent) {
        auditLog.debug("redispatchBatch", batchEvent);
        for (Object eventDto : batchEvent.getBatchData()) {
            auditLog.debug("redispatchEvent", eventDto);
            context.getParentDataFlow().onEvent(eventDto);
        }
        return false;
    }
}
