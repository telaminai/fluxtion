/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
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
