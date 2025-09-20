/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.time;

import lombok.Getter;

/**
 * @author 2024 gregory higgins.
 */
public interface ClockStrategy {

    long getWallClockTime();

    static ClockStrategyEvent registerClockEvent(ClockStrategy clock) {
        return new ClockStrategyEvent(clock);
    }

    @Getter
    class ClockStrategyEvent {

        private final ClockStrategy strategy;

        public ClockStrategyEvent(ClockStrategy strategy) {
            this.strategy = strategy;
        }

    }

}
