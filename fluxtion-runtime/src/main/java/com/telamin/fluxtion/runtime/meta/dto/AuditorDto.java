/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * Serializable representation of an auditor binding, carrying the metadata
 * the server-side model generator needs without access to the live
 * {@code Auditor} instance. Replaces the previous {@code String} (className)
 * value in {@link TopologicallySortedDependencyGraphDto#getAuditorMap()}.
 *
 * <p>{@code auditInvocations} mirrors {@code Auditor#auditInvocations()} —
 * it gates {@code nodeInvoked} call-site emission in the generated SEP.
 * Without it travelling across the wire the cloud-generated SEP defaults
 * the flag to {@code false} and silently drops per-node audit dispatch.
 */
public final class AuditorDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String className;
    private final boolean auditInvocations;

    public AuditorDto(String className, boolean auditInvocations) {
        this.className = Objects.requireNonNull(className, "className");
        this.auditInvocations = auditInvocations;
    }

    public String getClassName() {
        return className;
    }

    public boolean isAuditInvocations() {
        return auditInvocations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditorDto)) return false;
        AuditorDto that = (AuditorDto) o;
        return auditInvocations == that.auditInvocations
                && Objects.equals(className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, auditInvocations);
    }

    @Override
    public String toString() {
        return "AuditorDto{className=" + className
                + ", auditInvocations=" + auditInvocations + "}";
    }
}
