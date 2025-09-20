/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.util;

import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;

import java.util.ArrayList;
import java.util.List;

public class ObjectPool<T> {

    private final SerializableSupplier<T> supplier;
    private transient final List<T> freeList = new ArrayList<>();

    public ObjectPool(SerializableSupplier<T> supplier) {
        this.supplier = supplier;
    }

    public T checkOut() {
        if (freeList.isEmpty()) {
            return supplier.get();
        }
        return freeList.remove(freeList.size() - 1);
    }

    public void checkIn(T returnedInstance) {
        freeList.add(returnedInstance);
    }

    public void reset() {
        freeList.clear();
    }
}
