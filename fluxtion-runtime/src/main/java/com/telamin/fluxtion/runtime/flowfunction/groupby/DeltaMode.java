/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

/**
 * How a {@link GroupByDelta}'s entries relate to the consumer's existing derived state.
 *
 * <p>One API, three modes — see {@code docs/design/groupby-delta-ivm.md}. A full clear is simply
 * {@link #CLEAR_THEN_APPLY} with no entries; a reset-with-basis is {@link #CLEAR_THEN_APPLY} with
 * entries. There is no separate reset / clear / tombstone / retraction API in the core.
 */
public enum DeltaMode {
    /** Apply the entries on top of the existing derived state. */
    INCREMENTAL,
    /**
     * Clear the derived state first, then apply the entries as the new basis. Entries may be empty
     * (cleared-to-empty, e.g. a tumbling-window reset with no carry-over). This mode is contagious: a
     * consumer that clears must re-emit {@code CLEAR_THEN_APPLY} downstream so the whole chain rebuilds
     * in step.
     */
    CLEAR_THEN_APPLY,
    /**
     * No delta information is available; the consumer must recompute (or diff) from
     * {@link GroupBy#toMap()}. This is the safe default — a delta is an optimisation over recompute,
     * never over correctness.
     */
    RECOMPUTE_REQUIRED
}