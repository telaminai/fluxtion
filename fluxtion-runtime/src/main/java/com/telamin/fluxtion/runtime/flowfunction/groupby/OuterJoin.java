/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

public class OuterJoin extends AbstractJoin {

    /** Full outer: a key is in the join when either side has it (the absent side's value is null). */
    @Override
    protected boolean included(boolean leftPresent, boolean rightPresent) {
        return leftPresent || rightPresent;
    }
}
