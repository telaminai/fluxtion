/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.eventfeed;

import com.telamin.fluxtion.runtime.annotations.feature.Experimental;

@Experimental
public enum ReadStrategy {
    COMMITED,
    EARLIEST,
    LATEST,
    ONCE_EARLIEST,
    ONCE_LATEST;
}
