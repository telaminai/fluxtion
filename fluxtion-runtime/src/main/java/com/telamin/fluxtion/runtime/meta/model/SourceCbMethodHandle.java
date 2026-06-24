/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.meta.model;

import java.io.Serializable;

public interface SourceCbMethodHandle extends Serializable {
    String getMethodTarget();

    String getMethodName();

    int getParameterCount();

    String getReturnType();

    String getVariableName();

    String getParameterClass();

    boolean isEventHandler();

    boolean isExportedHandler();

    boolean isPostEventHandler();

    boolean isInvertedDirtyHandler();

    boolean isGuardedParent();

    boolean isNoPropagateEventHandler();

    boolean isForkExecution();

    String invokeLambdaString();

    String forkVariableName();
}
