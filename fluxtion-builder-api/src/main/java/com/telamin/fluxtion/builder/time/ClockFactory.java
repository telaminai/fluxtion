/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.builder.time;

import com.telamin.fluxtion.builder.node.NodeFactory;
import com.telamin.fluxtion.builder.node.NodeRegistry;
import com.telamin.fluxtion.runtime.time.Clock;

import java.util.Map;

/**
 * @author 2024 gregory higgins.
 */
public class ClockFactory implements NodeFactory<Clock> {

    @Override
    public Clock createNode(Map<String, ? super Object> config, NodeRegistry registry) {
        registry.registerAuditor(Clock.DEFAULT_CLOCK, "clock");
        return Clock.DEFAULT_CLOCK;
    }

}
