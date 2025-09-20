/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.flowfunction;

import com.telamin.fluxtion.runtime.flowfunction.TriggeredFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.MapFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy;

public class AbstractGroupByBuilder<K, V, T extends GroupBy<K, V>> extends FlowBuilder<T> {

    AbstractGroupByBuilder(TriggeredFlowFunction<T> eventStream) {
        super(eventStream);
    }

    <I, G extends GroupBy<K, V>>
    AbstractGroupByBuilder(MapFlowFunction<I, T, TriggeredFlowFunction<I>> eventStream) {
        super(eventStream);
    }
}