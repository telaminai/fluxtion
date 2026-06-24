/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.output;

import com.telamin.fluxtion.runtime.event.DefaultEvent;

public class SinkDeregister extends DefaultEvent {

    private SinkDeregister(String sinkId) {
        super(sinkId);
    }

    public static SinkDeregister sink(String sinkId) {
        return new SinkDeregister(sinkId);
    }
}
