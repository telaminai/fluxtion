/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.node;


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