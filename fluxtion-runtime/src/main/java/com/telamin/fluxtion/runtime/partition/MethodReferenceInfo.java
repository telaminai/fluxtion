/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.partition;

/**
 * Generation-time-resolved metadata for a DSL operator's method reference.
 *
 * <p>The DSL flow-function runtime used to recover this metadata at construction
 * by reflecting on the lambda via {@link LambdaReflection.MethodReferenceReflection#serialized()}
 * ({@code writeReplace} / {@code SerializedLambda}). That reflection is
 * unsupported in closed-world AOT runtimes (GraalVM native-image, TeaVM/WASM).
 *
 * <p>The builder, which runs on a real JVM, resolves the metadata once at
 * generation time and emits a {@code MethodReferenceInfo} literal into the
 * generated source. The closed-world operator constructors read this carrier
 * directly, so the generated artefact never calls {@code serialized()}.
 *
 * <p>This is a plain carrier — deliberately <em>not</em> a
 * {@link LambdaReflection.MethodReferenceReflection} — because
 * {@code MethodReferenceReflection.method()} returns a {@link java.lang.reflect.Method}
 * that cannot be reconstituted closed-world. The operators only needed the
 * method name (audit label) and declaring-class trait (default-value supplier),
 * which are carried here as a {@link String} and a {@code boolean}.
 */
public final class MethodReferenceInfo {

    private final String auditName;
    private final boolean stateful;
    private final Object resetReference;
    private final boolean defaultValueSupplier;

    /**
     * @param auditName            audit-log label, e.g. {@code "DslFuncs->times2"}
     * @param stateful             whether the referenced function is a {@code Stateful}
     * @param resetReference       the captured stateful node to reset (or {@code null})
     * @param defaultValueSupplier whether the declaring class is a {@code DefaultValueSupplier}
     */
    public MethodReferenceInfo(String auditName, boolean stateful, Object resetReference, boolean defaultValueSupplier) {
        this.auditName = auditName;
        this.stateful = stateful;
        this.resetReference = resetReference;
        this.defaultValueSupplier = defaultValueSupplier;
    }

    public String getAuditName() {
        return auditName;
    }

    public boolean isStateful() {
        return stateful;
    }

    public Object getResetReference() {
        return resetReference;
    }

    public boolean isDefaultValueSupplier() {
        return defaultValueSupplier;
    }
}
