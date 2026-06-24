/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.model;

/**
 * Strategy for dispatching events to handlers in the generated event processor.
 */
public enum DispatchStrategy {
    CLASS_NAME,
    INSTANCE_OF,
    PATTERN_MATCH
}
