/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

public class IntAverageFlowFunction extends AbstractIntFlowFunction<IntAverageFlowFunction> {

    private final DoubleAverageFlowFunction avg = new DoubleAverageFlowFunction();

    @Override
    public void combine(IntAverageFlowFunction add) {
        avg.combine(add.avg);
        value = (int) avg.aggregateDouble(value);
    }

    @Override
    public void deduct(IntAverageFlowFunction add) {
        avg.deduct(add.avg);
        value = (int) avg.aggregateDouble(value);
    }

    @Override
    public int aggregateInt(int input) {
        value = (int) avg.aggregateDouble(input);
        return getAsInt();
    }

    @Override
    public int resetInt() {
        avg.resetDouble();
        value = (int) avg.getAsDouble();
        return getAsInt();
    }
}
