/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

public class InnerJoin extends AbstractJoin {

    /** Inner: a key is in the join only when both sides have it. */
    @Override
    protected boolean included(boolean leftPresent, boolean rightPresent) {
        return leftPresent && rightPresent;
    }
}
