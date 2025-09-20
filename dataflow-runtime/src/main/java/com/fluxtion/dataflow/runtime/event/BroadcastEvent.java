/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.checkerframework.checker.lock.qual.NewObject;

@Data
@AllArgsConstructor
@NewObject
public class BroadcastEvent {
    private Object event;
}
