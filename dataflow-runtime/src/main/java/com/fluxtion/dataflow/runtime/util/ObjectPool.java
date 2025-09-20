/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.util;

import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableSupplier;

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
