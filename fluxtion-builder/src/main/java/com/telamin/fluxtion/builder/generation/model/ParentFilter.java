/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.generation.model;

/**
 * A filter class for a parent class, a match will occur if both instances are
 * equal or both instances are null and the class types are equal.
 *
 * @author Greg Higgins
 */
class ParentFilter {

    public Class<?> parentType;
    public String parentName;
    public CbMethodHandle callBack;

    public ParentFilter(Class<?> parentType, String parentName, CbMethodHandle callBack) {
        this.parentType = parentType;
        this.parentName = parentName;
        this.callBack = callBack;
    }

    public boolean match(ParentFilter other) {
        if (other.parentName == null || parentName == null || other.parentName.length() == 0 || parentName.length() == 0) {
            return parentType.isAssignableFrom(other.parentType) || other.parentType.isAssignableFrom(parentType);
        }
        return other.parentName.equals(parentName) &&
                (other.parentType.isAssignableFrom(parentType) || parentType.isAssignableFrom(other.parentType));
    }

    public boolean exactmatch(ParentFilter other) {
        if (other.parentName == null & parentName == null) {
            return parentType == (other.parentType);
        }

        if (other.parentName != null & parentName != null) {
            return other.parentName.equals(parentName) && other.parentType == parentType;
        }
        return false;
    }

}
