/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.util;

/**
 * Mutable numeric value
 *
 * @author Greg Higgins
 */
public class MutableNumber extends Number {

    public int intValue;
    public long longValue;
    public double doubleValue;

    public static MutableNumber fromInt(int number) {
        MutableNumber mutableNumber = new MutableNumber();
        mutableNumber.set(number);
        return mutableNumber;
    }

    public static MutableNumber fromLong(long number) {
        MutableNumber mutableNumber = new MutableNumber();
        mutableNumber.set(number);
        return mutableNumber;
    }

    public static MutableNumber fromDouble(double number) {
        MutableNumber mutableNumber = new MutableNumber();
        mutableNumber.set(number);
        return mutableNumber;
    }

    @Override
    public float floatValue() {
        return (float) doubleValue();
    }

    @Override
    public int intValue() {
        return intValue;
    }

    @Override
    public long longValue() {
        return longValue;
    }

    @Override
    public double doubleValue() {
        return doubleValue;
    }

    public void set(Number number) {
        intValue = number.intValue();
        longValue = number.longValue();
        doubleValue = number.doubleValue();
    }

    public void set(int value) {
        setIntValue(value);
    }

    public void set(long value) {
        setLongValue(value);
    }

    public void set(double value) {
        setDoubleValue(value);
    }

    public void setCharValue(char charValue) {
        setIntValue(charValue);
    }

    public void setByteValue(byte byteValue) {
        setIntValue(byteValue);
    }

    public void setShortValue(short shortValue) {
        setIntValue(shortValue);
    }

    public void setIntValue(int intValue) {
        this.intValue = intValue;
        this.longValue = intValue;
        this.doubleValue = intValue;
    }

    public void setLongValue(long longValue) {
        this.longValue = longValue;
        this.intValue = (int) longValue;
        this.doubleValue = longValue;
    }

    public void setfloatValue(float floatValue) {
        setDoubleValue(floatValue);
    }

    public void setDoubleValue(double doubleValue) {
        this.doubleValue = doubleValue;
        this.longValue = (long) doubleValue;
        this.intValue = (int) doubleValue;
    }

//    public void copyFrom(NumericValue source){
//        intValue = source.intValue();
//        longValue = source.longValue();
//        doubleValue = source.doubleValue();
//    }

    public void copyFrom(Number source) {
        intValue = source.intValue();
        longValue = source.longValue();
        doubleValue = source.doubleValue();
    }

    @Override
    public int hashCode() {
        return this.intValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Number other = (Number) obj;
        if (this.intValue != other.intValue()) {
            return false;
        }
        if (this.longValue != other.longValue()) {
            return false;
        }
        if (Double.doubleToLongBits(this.doubleValue) != Double.doubleToLongBits(other.doubleValue())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MutableNumber{" + "intValue=" + intValue + ", longValue=" + longValue + ", doubleValue=" + doubleValue + '}';
    }

}
