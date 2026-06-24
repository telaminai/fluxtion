/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.meta.model;

import com.telamin.fluxtion.runtime.flowfunction.Tuple;
import com.telamin.fluxtion.runtime.flowfunction.groupby.MutableTuple;
import lombok.Getter;

import com.telamin.fluxtion.runtime.meta.dto.MethodDescriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ExportFunctionData implements Serializable {

    private static final long serialVersionUID = 1L;

    private final MethodDescriptor methodDescriptor;
    private final List<Tuple<CbMethodHandle, Boolean>> functionCallBackList = new ArrayList<>();

    public ExportFunctionData(MethodDescriptor methodDescriptor) {
        this.methodDescriptor = methodDescriptor;
    }

    public void addCbMethodHandle(CbMethodHandle cbMethodHandle, boolean propagateClass) {
        functionCallBackList.add(new MutableTuple<>(cbMethodHandle, propagateClass));
    }

    public boolean isBooleanReturn() {
        for (int i = 0, functionCallBackListSize = functionCallBackList.size(); i < functionCallBackListSize; i++) {
            CbMethodHandle cbMethodHandle = functionCallBackList.get(i).getFirst();
            if ("boolean".equals(cbMethodHandle.getReturnType())) {
                return true;
            }
        }
        return false;
    }
}
