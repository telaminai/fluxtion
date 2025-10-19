/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.compile.serialiser;


import com.telamin.fluxtion.builder.generation.serialiser.FieldContext;
import org.apache.commons.text.StringEscapeUtils;

public interface BasicTypeSerializer {

    static String stringToSource(FieldContext<String> fieldContext) {
        return "\"" + StringEscapeUtils.escapeJava(fieldContext.getInstanceToMap()) + "\"";
    }

    static String charToSource(FieldContext<Character> fieldContext) {
        return "'" + StringEscapeUtils.escapeJava(fieldContext.getInstanceToMap().toString()) + "'";
    }

    static String longToSource(FieldContext<Long> fieldContext) {
        return fieldContext.getInstanceToMap().toString() + "L";
    }

    static String intToSource(FieldContext<Integer> fieldContext) {
        return fieldContext.getInstanceToMap().toString();
    }

    static String shortToSource(FieldContext<Short> fieldContext) {
        return "(short)" + fieldContext.getInstanceToMap().toString();
    }

    static String byteToSource(FieldContext<Byte> fieldContext) {
        return "(byte)" + fieldContext.getInstanceToMap().toString();
    }

    static String doubleToSource(FieldContext<Double> fieldContext) {
        Double doubleVal = fieldContext.getInstanceToMap();
        return doubleVal.isNaN() ? "Double.NaN" : doubleVal.toString();
    }

    static String floatToSource(FieldContext<Float> fieldContext) {
        return fieldContext.getInstanceToMap().toString() + "f";
    }

    static String booleanToSource(FieldContext<Boolean> fieldContext) {
        return fieldContext.getInstanceToMap().toString();
    }
}
