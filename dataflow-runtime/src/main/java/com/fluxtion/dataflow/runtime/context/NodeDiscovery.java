/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.context;

public interface NodeDiscovery {

    <T> T getNodeById(String id) throws NoSuchFieldException;
}
