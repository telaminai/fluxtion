/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
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
