/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.node;

public interface NodeNameLookup {
    String DEFAULT_NODE_NAME = "nodeNameLookup";

    String lookupInstanceName(Object node);

    <T> T getInstanceById(String id) throws NoSuchFieldException;
}
