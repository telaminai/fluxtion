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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public interface FormatSerializer {

    static String simpleDataFormatToSource(FieldContext<SimpleDateFormat> fieldContext) {
        fieldContext.getImportList().add(SimpleDateFormat.class);
        SimpleDateFormat uri = fieldContext.getInstanceToMap();
        return "new SimpleDateFormat(" +
                "\"" + StringEscapeUtils.escapeJava(uri.toLocalizedPattern()) + "\")";
    }

    static String decimalFormatToSource(FieldContext<DecimalFormat> fieldContext) {
        fieldContext.getImportList().add(DecimalFormat.class);
        DecimalFormat uri = fieldContext.getInstanceToMap();
        return "new DecimalFormat(" +
                "\"" + StringEscapeUtils.escapeJava(uri.toPattern()) + "\")";
    }

}
