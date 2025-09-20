/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.output;

import com.fluxtion.dataflow.runtime.event.DefaultEvent;

public class SinkDeregister extends DefaultEvent {

    private SinkDeregister(String sinkId) {
        super(sinkId);
    }

    public static SinkDeregister sink(String sinkId) {
        return new SinkDeregister(sinkId);
    }
}
