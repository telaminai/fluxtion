/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

import com.telamin.fluxtion.runtime.flowfunction.MutableNumber;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;

public abstract class AbstractMutableNumberFlowFunction
        implements AggregateFlowFunction<Number, Number, AbstractMutableNumberFlowFunction> {
    MutableNumber mutableNumber = new MutableNumber();

    @Override
    public Number reset() {
        return mutableNumber.reset();
    }

    @Override
    public void combine(AbstractMutableNumberFlowFunction add) {

    }

    @Override
    public void deduct(AbstractMutableNumberFlowFunction add) {

    }

    @Override
    public Number get() {
        return mutableNumber;
    }

    @Override
    public Number aggregate(Number input) {
        return null;
    }
}
