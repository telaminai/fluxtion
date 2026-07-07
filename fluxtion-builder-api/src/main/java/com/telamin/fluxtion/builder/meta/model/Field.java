/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.builder.meta.model;

import com.telamin.fluxtion.runtime.audit.Auditor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Greg Higgins
 */
public class Field implements SourceField, Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final String fqn;
    private final boolean publicAccess;
    private transient Object instance;
    private final String fieldClassName;
    private boolean auditor;
    private boolean auditInvocations;
    private final boolean generic;

    public Field(String fqn, String name, Object instance, boolean publicAccess) {
        this.fqn = fqn;
        this.name = name;
        this.instance = instance;
        this.publicAccess = publicAccess;
//        this.fieldClass = instance == null ? null : instance.getClass();
        this.fieldClassName = instance == null ? null : instance.getClass().getCanonicalName();
        if (instance instanceof Auditor) {
            auditor = true;
            auditInvocations = ((Auditor) instance).auditInvocations();
        } else {
            auditor = false;
            auditInvocations = false;
        }
        this.generic = instance != null && instance.getClass().getTypeParameters().length > 0;
    }

    /**
     * Constructor for the remote/DTO source-gen path where no live instance
     * is available and the audit flags have already been resolved on the
     * client side. Avoids the {@code instanceof Auditor} test on a null
     * instance which would otherwise force {@code auditInvocations=false}
     * and silently disable per-node {@code nodeInvoked} emission.
     */
    public Field(String fqn, String name, boolean publicAccess, boolean isAuditor, boolean auditInvocations) {
        this.fqn = fqn;
        this.name = name;
        this.instance = null;
        this.publicAccess = publicAccess;
        this.fieldClassName = null;
        this.auditor = isAuditor;
        this.auditInvocations = auditInvocations;
        this.generic = false;
    }

    @Override
    public boolean isGeneric() {
        return generic;
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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getFqn() {
        return fqn;
    }

    @Override
    public boolean isPublicAccess() {
        return publicAccess;
    }

    public Object getInstance() {
        return instance;
    }

    /** Sets the live instance reference for this field. Used by {@code resolveInstances()} after DTO-based model generation. */
    public void setInstance(Object instance) {
        this.instance = instance;
        // Recalculate auditor flags when instance is resolved (DTO path constructs Fields with null instance)
        if (instance instanceof Auditor) {
            auditor = true;
            auditInvocations = ((Auditor) instance).auditInvocations();
        }
    }

    @Override
    public String getFieldClassName() {
        return fieldClassName;
    }

    @Override
    public boolean isAuditor() {
        return auditor;
    }

    @Override
    public boolean isAuditInvocations() {
        return auditInvocations;
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