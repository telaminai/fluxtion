/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.output;

import com.telamin.fluxtion.builder.node.NodeFactory;
import com.telamin.fluxtion.builder.node.NodeRegistry;
import com.telamin.fluxtion.runtime.output.SinkPublisher;

import java.util.Map;

public class SinkPublisherFactory implements NodeFactory<SinkPublisher> {

    @Override
    public SinkPublisher<?> createNode(Map<String, Object> config, NodeRegistry registry) {
        final String instanceName = (String) config.get(NodeFactory.INSTANCE_KEY);
        return new SinkPublisher<>(instanceName);
    }

}
