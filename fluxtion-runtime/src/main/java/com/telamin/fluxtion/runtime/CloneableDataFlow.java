/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime;

import com.telamin.fluxtion.runtime.lifecycle.Lifecycle;

import java.util.Map;

public interface CloneableDataFlow<T extends CloneableDataFlow<?>> extends DataFlow, Lifecycle {

    default T newInstance() {
        throw new UnsupportedOperationException();
    }

    default T newInstance(Map<Object, Object> contextMap) {
        throw new UnsupportedOperationException();
    }

}
