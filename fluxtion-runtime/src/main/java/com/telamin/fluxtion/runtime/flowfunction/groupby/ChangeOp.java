/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

/**
 * The kind of change a {@link Change} carries within a {@link GroupByDelta}.
 *
 * <p>{@code ADD} and {@code UPDATE} are both applied as a map {@code put}; the distinction is kept
 * deliberately because it is load-bearing for downstream consumers that care about it — e.g.
 * action/push sinks ({@code onInsert} vs {@code onUpdate}) and operators computing their own
 * downstream change op. {@code DELETE} removes the key (it left the group, or an operator's predicate
 * flipped it from passing to failing).
 */
public enum ChangeOp {
    /** A key that was not previously in the group. */
    ADD,
    /** A key already in the group whose value changed. */
    UPDATE,
    /** A key that left the group (eviction / expiry / delete / now-failing a filter). */
    DELETE
}