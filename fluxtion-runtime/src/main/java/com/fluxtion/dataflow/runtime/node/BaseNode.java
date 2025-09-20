/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.node;

import com.fluxtion.dataflow.runtime.context.DataFlowContext;
import com.fluxtion.dataflow.runtime.context.DataFlowContextListener;
import com.fluxtion.dataflow.runtime.annotations.Initialise;
import com.fluxtion.dataflow.runtime.annotations.builder.FluxtionIgnore;
import com.fluxtion.dataflow.runtime.audit.EventLogNode;
import lombok.Getter;
import lombok.Setter;

public class BaseNode extends EventLogNode implements DataFlowContextListener {

    @Getter
    protected DataFlowContext context;
    @Getter
    @Setter
    @FluxtionIgnore
    private String name;

    @Initialise
    public final void init() {
        name = context.getNodeNameLookup().lookupInstanceName(this);
        auditLog.info("init", name);
        _initialise();
    }

    protected void _initialise() {
    }

    @Override
    public final void currentContext(DataFlowContext dataFlowContext) {
        this.context = dataFlowContext;
    }
}
