/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
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
