/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
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
