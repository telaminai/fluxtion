/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.node;

import com.fluxtion.dataflow.runtime.CloneableDataFlow;

/**
 * A unique name for a node in an instance {@link CloneableDataFlow}. Advised to return a human-readable name that will make debugging
 * and code generation easier to interpret. Has no semantic meaning.
 */
public interface NamedNode {
    String getName();
}
