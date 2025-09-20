/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.callback;

import com.fluxtion.dataflow.runtime.audit.EventLogNode;
import com.fluxtion.dataflow.runtime.node.NamedNode;

import java.util.Objects;

public abstract class NamedNodeSimple extends EventLogNode implements NamedNode {

    private String name;

    public NamedNodeSimple(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamedNodeSimple that = (NamedNodeSimple) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
