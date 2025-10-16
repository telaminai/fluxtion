/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.generation.model;

import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Greg Higgins
 */
public class Field {

    private final String name;
    private final String fqn;
    private final boolean publicAccess;
    private final Object instance;
    private final Class<?> fieldClass;

    public Field(String fqn, String name, Object instance, boolean publicAccess) {
        this.fqn = fqn;
        this.name = name;
        this.instance = instance;
        this.publicAccess = publicAccess;
        this.fieldClass = instance == null ? null : instance.getClass();
    }

    public boolean isGeneric() {
        TypeVariable<? extends Class<?>>[] typeParameters = instance.getClass().getTypeParameters();
        return typeParameters.length > 0;
    }

    @Override
    public String toString() {
        return "Field{"
                + "name=" + name
                + ", fqn=" + fqn
                + ", publicAccess=" + publicAccess
                + ", instance=" + instance
                + '}';
    }

    public String getName() {
        return name;
    }

    public String getFqn() {
        return fqn;
    }

    public boolean isPublicAccess() {
        return publicAccess;
    }

    public Object getInstance() {
        return instance;
    }

    public Class<?> getFieldClass() {
        return fieldClass;
    }

    public static class MappedField extends Field {

        private final String mappedName;
        private final boolean collection;
        private boolean primitive = false;
        private Object primitiveVal;
        private ArrayList<Field> elements;
        private String derivedVal;
        private Class<?> collectionClass;

        public MappedField(String mappedName, Field f) {
            super(f.fqn, f.name, f.instance, f.publicAccess);
            this.mappedName = mappedName;
            Class<?> aClass = f.instance.getClass();
            collection = List.class.isAssignableFrom(aClass);
            elements = new ArrayList<>();
        }

        public MappedField(String mappedName, Class<?> collectionClass) {
            super(collectionClass.getName(), null, null, false);
            this.collectionClass = collectionClass;
            this.mappedName = mappedName;
            collection = true;
            elements = new ArrayList<>();
        }

        public MappedField(String mappedName, Object primitiveValue) {
            super(null, null, null, false);
            this.mappedName = mappedName;
            collection = false;
            primitive = true;
            primitiveVal = primitiveValue;
        }

        public Class<?> parentClass() {
            if (collection) {
                return collectionClass;
            } else if (primitive) {
                if (primitiveVal.getClass() == Integer.class) {
                    return int.class;
                }
                if (primitiveVal.getClass() == Double.class) {
                    return double.class;
                }
                if (primitiveVal.getClass() == Float.class) {
                    return float.class;
                }
                if (primitiveVal.getClass() == Byte.class) {
                    return byte.class;
                }
                if (primitiveVal.getClass() == Short.class) {
                    return short.class;
                }
                if (primitiveVal.getClass() == Long.class) {
                    return long.class;
                }
                if (primitiveVal.getClass() == Boolean.class) {
                    return boolean.class;
                }
                if (primitiveVal.getClass() == Character.class) {
                    return char.class;
                }
                return primitiveVal.getClass();
            } else {
                return getInstance().getClass();
            }
        }

        public Class<?> realClass() {
            if (collection) {
                return collectionClass;
            } else if (primitive) {
                return primitiveVal.getClass();
            } else {
                return getInstance().getClass();
            }
        }

        public String value() {
            return getDerivedVal();
        }

        public void addField(Field field) {
            if (field != null) {
                elements.add(field);
            }
        }

        public String getMappedName() {
            return mappedName;
        }

        public boolean isEmpty() {
            return elements.isEmpty();
        }

        @Override
        public String toString() {
            return "MappedField{"
                    + "mappedName=" + mappedName
                    + ", name=" + getName()
                    + ", collection=" + collection
                    + ", fqn=" + getFqn()
                    + ", publicAccess=" + isPublicAccess()
                    + ", instance=" + getInstance()
                    + '}';
        }

        public String getDerivedVal() {
            return derivedVal;
        }

        public void setDerivedVal(String derivedVal) {
            this.derivedVal = derivedVal;
        }
    }
}