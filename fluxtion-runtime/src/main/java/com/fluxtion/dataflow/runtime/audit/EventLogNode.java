/*
 * SPDX-File Copyright: © 2020-2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.audit;

/**
 * Base class that adds {@link EventLogger} functionality.
 *
 * @author Greg Higgins greg.higgins@v12technology.com
 */
public class EventLogNode implements EventLogSource {

    protected EventLogger auditLog = NullEventLogger.INSTANCE;

    @Override
    public void setLogger(EventLogger log) {
        this.auditLog = log;
    }

}
