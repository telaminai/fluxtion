/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.groupby;

import com.fluxtion.dataflow.runtime.flowfunction.FlowSupplier;
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
