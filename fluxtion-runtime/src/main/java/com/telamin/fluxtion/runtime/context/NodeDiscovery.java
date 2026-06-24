/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.context;

public interface NodeDiscovery {

    <T> T getNodeById(String id) throws NoSuchFieldException;
}
