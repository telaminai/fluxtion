/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.flowfunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StreamHelper {
    public static Object getSource(Object input) {
        Object returnValue = input;
        if (input instanceof FlowDataSupplier) {
            returnValue = ((FlowDataSupplier) input).flowSupplier();
        }
        return returnValue;
    }

    public static List<Object> getSourcesAsList(Object... inputs) {
        ArrayList<Object> list = new ArrayList<>();
        if (inputs != null) {
            Arrays.stream(inputs).map(StreamHelper::getSource).forEach(list::add);
        }
        return list;
    }

}
