/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.callback;

import com.telamin.fluxtion.runtime.event.Event;

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
