/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.runtime.node;


/**
 * @param <T> The type of event processed by this handler
 * @author Greg Higgins
 */
public abstract class AbstractEventHandlerNode<T> implements EventHandlerNode<T> {

    protected int filterId;

    public AbstractEventHandlerNode(int filterId) {
        this.filterId = filterId;
    }

    public AbstractEventHandlerNode() {
        this(0);
    }

    @Override
    public final int filterId() {
        return filterId;
    }

    public void setFilterId(int filterId) {
        this.filterId = filterId;
    }
}