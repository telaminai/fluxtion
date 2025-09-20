/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.callback;

import com.fluxtion.dataflow.builder.node.NodeFactory;
import com.fluxtion.dataflow.builder.node.NodeRegistry;
import com.fluxtion.dataflow.runtime.callback.CallBackNode;
import com.fluxtion.dataflow.runtime.callback.Callback;
import com.fluxtion.dataflow.runtime.callback.CallbackImpl;

import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

public class CallbackNodeFactory implements NodeFactory<Callback> {
    private static final LongAdder idGenerator = new LongAdder();

    @Override
    public Callback<?> createNode(Map<String, Object> config, NodeRegistry registry) {

        try {
            return new CallBackNode<>();
        } catch (Throwable e) {
            idGenerator.increment();
            return new CallbackImpl<>(idGenerator.intValue());
        }

    }
}
