/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
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
