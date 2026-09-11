/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.builder.meta.model;

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
    /**
     * The node's captured scalar field state, for targets that cannot reflect.
     *
     * <p>Empty by default, so every existing implementation and every existing target is unaffected:
     * the Java generator keeps reflecting over the live instance as it always has, and a target that
     * ignores this list behaves exactly as before.
     *
     * <p>It exists because {@link Field#getInstance()} is transient and this interface carries no
     * values, which together meant a non-Java target received none of a node's state and could not
     * know it was missing. See {@link FieldValue}.
     *
     * @return captured scalar fields, never null
     */
    default java.util.List<FieldValue> getFieldValues() {
        return java.util.Collections.emptyList();
    }

    boolean isAuditor();

    /**
     * Checks if this field audits invocations.
     *
     * @return true if field audits invocations, false otherwise
     */
    boolean isAuditInvocations();

    /**
     * Whether this auditor wants the per-event callbacks — the event-path counterpart of
     * {@link #isAuditInvocations()}. Defaulted to {@code true} so an implementor written before
     * this method existed keeps the behaviour it had.
     *
     * @return true when the generated processor should call this auditor on the event path
     */
    default boolean isAuditEventReceipt() {
        return true;
    }
}
