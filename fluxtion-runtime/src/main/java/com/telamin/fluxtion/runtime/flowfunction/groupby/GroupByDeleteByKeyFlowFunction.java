/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.flowfunction.FlowSupplier;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Data
public class GroupByDeleteByKeyFlowFunction {

    private final FlowSupplier<?> keysToDelete;
    private final boolean remove;
    @SuppressWarnings("rawtypes")
    private final transient GroupByHashMap deltaView = new GroupByHashMap();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public GroupBy deleteByKey(GroupBy groupBy, Collection keysToDelete) {
        if (this.keysToDelete.hasChanged() && !keysToDelete.isEmpty()) {
            Map map = groupBy.toMap();
            // Emit an explicit DELETE (carrying the previous value) for each removed key so a delete-aware
            // consumer (changes()) sees the removal — merged with this cycle's upstream ADD/UPDATE delta. Only
            // when the upstream is incremental; a RECOMPUTE_REQUIRED upstream re-diffs from toMap() downstream,
            // where the removed keys are simply absent.
            GroupByDelta upstream = groupBy.delta();
            boolean incremental = upstream.mode() != DeltaMode.RECOMPUTE_REQUIRED;
            List<Change> merged = incremental ? new ArrayList<>(upstream.entries()) : null;
            boolean anyDeleted = false;
            for (Object key : keysToDelete) {
                if (map.containsKey(key)) {
                    if (incremental) {
                        merged.add(Change.delete(key, map.get(key)));
                    }
                    anyDeleted = true;
                }
            }
            map.keySet().removeAll(keysToDelete);
            if (remove) {
                keysToDelete.clear();
            }
            if (anyDeleted && incremental) {
                deltaView.fromMap(map);
                deltaView.setDelta(GroupByDelta.incremental((List) merged));
                return deltaView;
            }
        }
        return groupBy;
    }
}
