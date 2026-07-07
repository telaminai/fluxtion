/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.builder.meta.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * A simple class name DTO for remote generation. Contains the fully qualified
 * name and simple name of a class.
 */
public class ClassName implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String fqn;
    private final String simpleName;

    public ClassName(String fqn) {
        this.fqn = fqn;
        int lastDot = fqn.lastIndexOf('.');
        this.simpleName = lastDot == -1 ? fqn : fqn.substring(lastDot + 1);
    }

    public ClassName(String fqn, String simpleName) {
        this.fqn = fqn;
        this.simpleName = simpleName;
    }

    public static ClassName of(Class<?> clazz) {
        return new ClassName(clazz.getName());
    }

    public String getFqn() {
        return fqn;
    }

    public String getSimpleName() {
        return simpleName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassName className = (ClassName) o;
        return Objects.equals(fqn, className.fqn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fqn);
    }

    @Override
    public String toString() {
        return fqn;
    }
}
