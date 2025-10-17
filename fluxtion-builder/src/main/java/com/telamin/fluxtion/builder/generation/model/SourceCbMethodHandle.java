/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.generation.model;

public interface SourceCbMethodHandle {
    String getMethodTarget();

    String getMethodName();

    int getParameterCount();

    Class<?> getReturnType();

    String getVariableName();

    Class<?> getParameterClass();

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
