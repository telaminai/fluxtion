/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.builder.callback;

import com.telamin.fluxtion.builder.node.NodeFactory;
import com.telamin.fluxtion.builder.node.NodeRegistry;
import com.telamin.fluxtion.runtime.callback.CallBackNode;
import com.telamin.fluxtion.runtime.callback.Callback;
import com.telamin.fluxtion.runtime.callback.CallbackImpl;

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
