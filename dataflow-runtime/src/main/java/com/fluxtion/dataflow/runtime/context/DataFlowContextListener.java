/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.context;

/**
 * Listener for the current {@link DataFlowContext}
 */
public interface DataFlowContextListener {

    void currentContext(DataFlowContext currentContext);
}
