/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.flowfunction.FlowSupplier;
import lombok.Data;

import java.util.Collection;

@Data
public class GroupByDeleteByKeyFlowFunction {

    private final FlowSupplier<?> keysToDelete;
    private final boolean remove;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public GroupBy deleteByKey(GroupBy groupBy, Collection keysToDelete) {
        if (this.keysToDelete.hasChanged()) {
            groupBy.toMap().keySet().removeAll(keysToDelete);
            if (remove) {
                keysToDelete.clear();
            }
        }
        return groupBy;
    }
}
