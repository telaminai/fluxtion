/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.callback;

import com.fluxtion.dataflow.runtime.event.Event;

public class ExportFunctionAuditEvent implements Event {
    private String functionDescription;


    public ExportFunctionAuditEvent setFunctionDescription(String functionDescription) {
        this.functionDescription = functionDescription;
        return this;
    }

    @Override
    public String toString() {
        return functionDescription;
    }
}
