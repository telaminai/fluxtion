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

    private int count;
    private int sum;

    @Override
    public int aggregateInt(int input) {
        sum += input;
        count++;
        value = sum / count;
        return getAsInt();
    }

    @Override
    public void combine(IntAverageFlowFunction combine) {
        sum += combine.sum;
        count += combine.count;
        value = sum / count;
    }

    @Override
    public void deduct(IntAverageFlowFunction deduct) {
        sum -= deduct.sum;
        count -= deduct.count;
        value = sum / count;
    }

    @Override
    public int resetInt() {
        super.resetInt();
        sum = 0;
        count = 0;
        return getAsInt();
    }

    @Override
    public String toString() {
        return "IntAverageFlowFunction{" +
                "avg=" + value +
                " count=" + count +
                ", sum=" + sum +
                ", value=" + value +
                ", reset=" + reset +
                '}';
    }
}