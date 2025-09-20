/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction;

public class MutableNumber extends Number {

    private int intValue;
    private double doubleValue;
    private long longValue;

    @Override
    public int intValue() {
        return intValue;
    }

    @Override
    public long longValue() {
        return longValue;
    }

    @Override
    public float floatValue() {
        return (float) doubleValue;
    }

    @Override
    public double doubleValue() {
        return doubleValue;
    }

    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    public void setDoubleValue(double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public void setLongValue(long longValue) {
        this.longValue = longValue;
    }

    public MutableNumber reset() {
        intValue = 0;
        doubleValue = 0;
        longValue = 0;
        return this;
    }
}
