/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.checkerframework.checker.lock.qual.NewObject;

@Data
@AllArgsConstructor
@NewObject
public class BroadcastEvent {
    private Object event;
}
