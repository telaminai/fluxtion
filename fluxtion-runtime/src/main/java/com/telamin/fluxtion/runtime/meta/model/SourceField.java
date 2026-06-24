/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.meta.model;

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
