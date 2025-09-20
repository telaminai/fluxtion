/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.node;

import com.telamin.fluxtion.runtime.context.DataFlowContext;
import com.telamin.fluxtion.runtime.context.DataFlowContextListener;
import com.telamin.fluxtion.runtime.annotations.Initialise;
import com.telamin.fluxtion.runtime.annotations.builder.FluxtionIgnore;
import com.telamin.fluxtion.runtime.audit.EventLogNode;
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
