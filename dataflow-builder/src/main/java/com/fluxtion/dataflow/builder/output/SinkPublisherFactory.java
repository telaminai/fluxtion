/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.output;

import com.fluxtion.dataflow.builder.node.NodeFactory;
import com.fluxtion.dataflow.builder.node.NodeRegistry;
import com.fluxtion.dataflow.runtime.output.SinkPublisher;

import java.util.Map;

public class SinkPublisherFactory implements NodeFactory<SinkPublisher> {

    @Override
    public SinkPublisher<?> createNode(Map<String, Object> config, NodeRegistry registry) {
        final String instanceName = (String) config.get(NodeFactory.INSTANCE_KEY);
        return new SinkPublisher<>(instanceName);
    }

}
