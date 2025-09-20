/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.generation.model;

import com.telamin.fluxtion.runtime.flowfunction.Tuple;
import com.telamin.fluxtion.runtime.flowfunction.groupby.MutableTuple;
import lombok.Getter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ExportFunctionData {

    private final Method exportedmethod;
    private final List<Tuple<CbMethodHandle, Boolean>> functionCallBackList = new ArrayList<>();

    public ExportFunctionData(Method exportedmethod) {
        this.exportedmethod = exportedmethod;
    }

    public void addCbMethodHandle(CbMethodHandle cbMethodHandle, boolean propagateClass) {
        functionCallBackList.add(new MutableTuple<>(cbMethodHandle, propagateClass));
    }

    public boolean isBooleanReturn() {
        for (int i = 0, functionCallBackListSize = functionCallBackList.size(); i < functionCallBackListSize; i++) {
            CbMethodHandle cbMethodHandle = functionCallBackList.get(i).getFirst();
            if (cbMethodHandle.getMethod().getReturnType() == boolean.class) {
                return true;
            }
        }
        return false;
    }
}
