/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.generation.config;

import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Configuration for a root node to be injected into the graph
 */
@Value
public class RootNodeConfig {
    String name;
    Class<?> rootClass;
    Map<String, Object> config;
    List<Object> nodes;
}
