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
import com.telamin.fluxtion.runtime.annotations.Initialise;
import com.telamin.fluxtion.runtime.annotations.TearDown;
import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import com.telamin.fluxtion.runtime.annotations.builder.SepNode;

@SepNode
public class InstanceSupplierNode<T> extends SingleNamedNode implements InstanceSupplier<T> {

    @Inject
    private final DataFlowContext context;
    private final boolean failFast;
    private final Object contextKey;
    private T instanceFromEventProcessorContext;

    public InstanceSupplierNode(Object contextKey) {
        this(contextKey, false, null);
    }

    public InstanceSupplierNode(
            Object contextKey,
            boolean failFast) {
        this(contextKey, failFast, null);
    }

    public InstanceSupplierNode(
            Object contextKey,
            boolean failFast,
            DataFlowContext context) {
        this(contextKey, failFast, context, "contextLookup_" + contextKey);
    }

    public InstanceSupplierNode(
            @AssignToField("contextKey") Object contextKey,
            @AssignToField("failFast") boolean failFast,
            @AssignToField("context") DataFlowContext context,
            @AssignToField("name") String name) {
        super(name.replace(".", "_"));
        this.contextKey = contextKey;
        this.failFast = failFast;
        this.context = context;
    }

    @Override
    public T get() {
        instanceFromEventProcessorContext = context.getContextProperty(contextKey);
        if (instanceFromEventProcessorContext == null && failFast) {
            throw new RuntimeException("missing context property for key:'" + contextKey + "'");
        }
        return instanceFromEventProcessorContext;
    }

    @Initialise
    public void init() {
        instanceFromEventProcessorContext = null;
        get();
    }

    @TearDown
    public void tearDown() {
        instanceFromEventProcessorContext = null;
    }
}
