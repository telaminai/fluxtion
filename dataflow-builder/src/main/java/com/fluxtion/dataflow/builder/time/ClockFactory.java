/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */
package com.fluxtion.dataflow.builder.time;

import com.fluxtion.dataflow.builder.node.NodeFactory;
import com.fluxtion.dataflow.builder.node.NodeRegistry;
import com.fluxtion.dataflow.runtime.time.Clock;

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
