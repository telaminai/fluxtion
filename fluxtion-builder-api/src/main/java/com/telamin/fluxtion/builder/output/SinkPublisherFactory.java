/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
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
