/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.time;

import com.telamin.fluxtion.runtime.event.Event;
import lombok.Data;

/**
 * A tick event notifies a clock to update its wall clock time. Any nodes that
 * depend upon clock will check their state and fire events as necessary.
 *
 * @author Greg Higgins greg.higgins@v12technology.com
 */
@Data
public class Tick implements Event {

    private long eventTime;
}
