/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.runtime.audit;

/**
 * Convenience base class for nodes that want to write structured audit log
 * key/value pairs via an {@link EventLogger}.
 * <p>
 * How to receive an EventLogger at runtime:
 * <ul>
 *   <li>Extend this class (recommended), or</li>
 *   <li>Implement {@link EventLogSource} directly.</li>
 * </ul>
 * The {@link EventLogManager} discovers all EventLogSource instances during
 * initialisation and injects a configured {@link EventLogger} by calling
 * {@link #setLogger(EventLogger)}. Until then, {@link #auditLog} is a
 * {@link NullEventLogger} that safely discards writes.
 * <p>
 * Typical usage in an event handler:
 * <pre>{@code
 * public class MyNode extends EventLogNode {
 *     @OnEventHandler
 *     public void onOrder(Order order) {
 *         auditLog.info("symbol", order.symbol())
 *                 .info("qty", order.qty())
 *                 .debug("calc", "enrich");
 *     }
 * }
 * }</pre>
 * To ensure your node receives an EventLogger, it must be part of the graph and
 * either extend EventLogNode or implement EventLogSource.
 *
 * @author Greg Higgins greg.higgins@v12technology.com
 */
public class EventLogNode implements EventLogSource {

    /**
     * Logger injected by {@link EventLogManager} during graph initialisation.
     * Defaults to {@link NullEventLogger} before injection.
     */
    protected EventLogger auditLog = NullEventLogger.INSTANCE;

    @Override
    public void setLogger(EventLogger log) {
        this.auditLog = log;
    }

}
