/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.callback;

import com.fluxtion.dataflow.runtime.event.Event;
import lombok.ToString;

@ToString
public class CallbackEvent<R> implements Event {
    protected int filterId;
    protected R data;

    @Override
    public int filterId() {
        return filterId;
    }

    public int getFilterId() {
        return filterId;
    }

    public void setFilterId(int filterId) {
        this.filterId = filterId;
    }

    public R getData() {
        return data;
    }

    public void setData(R data) {
        this.data = data;
    }
}
