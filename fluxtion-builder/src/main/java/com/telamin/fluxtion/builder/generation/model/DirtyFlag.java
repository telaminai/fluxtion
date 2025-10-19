/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.generation.model;

import java.util.Objects;

/**
 * A flag that represents the state of a node in a SEP.
 *
 * @author Greg Higgins
 */
public class DirtyFlag implements java.io.Serializable{
    public final Field node;
    public final String name;
    public boolean alwaysDirty;
    public boolean requiresInvert = false;

    public DirtyFlag(Field node, String name) {
        this.node = node;
        this.name = name;
        alwaysDirty = false;
        requiresInvert = false;
    }

    public DirtyFlag(Field node, String name, boolean alwaysDirty) {
        this.node = node;
        this.name = name;
        this.alwaysDirty = alwaysDirty;
    }

    public DirtyFlag clone() {
        DirtyFlag df = new DirtyFlag(node, name, alwaysDirty);
        df.requiresInvert = requiresInvert;
        return df;
    }

    public Field getNode() {
        return node;
    }

    public String getName() {
        return name;
    }

    public String getForkedName() {
        return "fork_" + node.getName();
    }

    public boolean isAlwaysDirty() {
        return alwaysDirty;
    }

    public boolean isRequiresInvert() {
        return requiresInvert;
    }

    @Override
    public String toString() {
        return "DirtyFlag{" + "node=" + node + ", name=" + name + ", defaultVal=" + alwaysDirty + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.node);
        hash = 53 * hash + Objects.hashCode(this.name);
        hash = 53 * hash + (this.alwaysDirty ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DirtyFlag other = (DirtyFlag) obj;
        if (this.alwaysDirty != other.alwaysDirty) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return Objects.equals(this.node, other.node);
    }

}
