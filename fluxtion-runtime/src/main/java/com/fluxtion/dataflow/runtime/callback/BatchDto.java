/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.callback;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Data Transfer Object (DTO) for batch operations.
 *
 * The BatchDto class is designed to manage a collection of batch items.
 * It provides functionality to add items to the batch for further processing.
 */
@Data
public class BatchDto {
    protected final List<Object> batchData = new ArrayList<>();

    public <T> void addBatchItem(T batchItem) {
        batchData.add(batchItem);
    }
}
