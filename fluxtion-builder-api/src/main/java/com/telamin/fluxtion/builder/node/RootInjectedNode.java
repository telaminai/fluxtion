/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.builder.node;

import lombok.Value;

import java.util.Map;

/**
 * Configuration for a root node to be injected into the graph
 */
@Value
public class RootInjectedNode {
    String name;
    Class<?> rootClass;
    Map<String, ?> config;
}
