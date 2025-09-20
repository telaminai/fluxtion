/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.callback;

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
