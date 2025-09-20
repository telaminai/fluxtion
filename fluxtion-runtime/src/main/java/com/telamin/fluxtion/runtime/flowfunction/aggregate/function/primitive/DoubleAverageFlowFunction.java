/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

public class DoubleAverageFlowFunction extends AbstractDoubleFlowFunction<DoubleAverageFlowFunction> {

    private int count;
    private double sum;

    @Override
    public double aggregateDouble(double input) {
        sum += input;
        count++;
        value = sum / count;
        return getAsDouble();
    }

    @Override
    public void combine(DoubleAverageFlowFunction combine) {
        sum += combine.sum;
        count += combine.count;
        value = sum / count;
    }

    @Override
    public void deduct(DoubleAverageFlowFunction deduct) {
        sum -= deduct.sum;
        count -= deduct.count;
        value = sum / count;
    }

    @Override
    public double resetDouble() {
        value = 0;
        sum = 0;
        count = 0;
        return 0;
    }
}
