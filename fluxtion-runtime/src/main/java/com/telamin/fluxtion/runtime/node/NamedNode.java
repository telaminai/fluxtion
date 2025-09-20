/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.node;

import com.telamin.fluxtion.runtime.CloneableDataFlow;

/**
 * A unique name for a node in an instance {@link CloneableDataFlow}. Advised to return a human-readable name that will make debugging
 * and code generation easier to interpret. Has no semantic meaning.
 */
public interface NamedNode {
    String getName();
}
