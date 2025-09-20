/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.fluxtion.dataflow.runtime.annotations;

/**
 * Filter match strategy for an {@link OnEventHandler}.
 * <p>
 * Available strategies are:
 * <ul>
 * <li> {@link FilterType#matched} Only matching filters allow event
 * propagation
 * <li> {@link FilterType#defaultCase} Invoked when no filter match is found,
 * acts like a default branch in a case statement.
 * </ul>
 *
 * @author Greg Higgins
 */
public enum FilterType {
    matched,
    defaultCase,
    ;
}
