/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.filter;

import lombok.Value;

@Value
public class EventHandlerFilterOverride {
    Object eventHandlerInstance;
    Class<?> eventType;
    int newFilterId;
}
