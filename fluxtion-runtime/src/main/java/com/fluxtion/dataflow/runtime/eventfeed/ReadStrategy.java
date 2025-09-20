/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.eventfeed;

import com.fluxtion.dataflow.runtime.annotations.feature.Experimental;

@Experimental
public enum ReadStrategy {
    COMMITED,
    EARLIEST,
    LATEST,
    ONCE_EARLIEST,
    ONCE_LATEST;
}
