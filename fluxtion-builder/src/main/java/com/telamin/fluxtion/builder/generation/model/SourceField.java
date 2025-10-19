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
 * Represents a field in a source model. It provides metadata about fields, such as
 * their names, fully-qualified names, access specifiers, types, and whether the
 * field is an entity used for auditing or generic.
 */
public interface SourceField {
    /**
     * Checks if the field is generic.
     *
     * @return true if the field is generic, false otherwise
     */
    boolean isGeneric();

    /**
     * Gets the simple name of the field.
     *
     * @return the simple name of the field
     */
    String getName();

    /**
     * Gets the fully qualified name of the field.
     *
     * @return the fully qualified name of the field
     */
    String getFqn();

    /**
     * Checks if the field has public access.
     *
     * @return true if field has public access, false otherwise
     */
    boolean isPublicAccess();

    /**
     * Gets the class name of the field type.
     *
     * @return the canonical name of the field's class type
     */
    String getFieldClassName();

    /**
     * Checks if this field is an auditor.
     *
     * @return true if field is an auditor, false otherwise
     */
    boolean isAuditor();

    /**
     * Checks if this field audits invocations.
     *
     * @return true if field audits invocations, false otherwise
     */
    boolean isAuditInvocations();
}
