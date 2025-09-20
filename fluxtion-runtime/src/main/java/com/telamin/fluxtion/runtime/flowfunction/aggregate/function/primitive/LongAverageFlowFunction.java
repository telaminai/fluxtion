/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

public class LongAverageFlowFunction extends AbstractLongFlowFunction<LongAverageFlowFunction> {

    private final DoubleAverageFlowFunction avg = new DoubleAverageFlowFunction();

    @Override
    public void combine(LongAverageFlowFunction add) {
        avg.combine(add.avg);
    }

    @Override
    public void deduct(LongAverageFlowFunction add) {
        avg.deduct(add.avg);
    }

    @Override
    public long aggregateLong(long input) {
        value = (long) avg.aggregateDouble(input);
        return getAsLong();
    }
}
