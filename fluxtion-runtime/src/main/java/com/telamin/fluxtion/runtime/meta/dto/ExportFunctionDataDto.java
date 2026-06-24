/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Serializable DTO describing an exported service function.
 * Replaces the live-object {@code ExportFunctionData} for crossing the network boundary.
 * Callbacks are identified by {@code "variableName::methodName"} keys — no live instances.
 */
public final class ExportFunctionDataDto implements Serializable {
    private static final long serialVersionUID = 3L;

    /** The interface method that is exported. */
    private final MethodDescriptor methodDescriptor;

    /**
     * Keys of the graph-node callbacks that implement this export, in the form
     * {@code "variableName::methodName"}.
     */
    private final List<String> callbackKeys;

    /** Whether the exported method has a {@code boolean} return type. */
    private final boolean booleanReturn;

    /**
     * The generated {@code @Override} method signature string used as the filter key
     * in the dispatch map for exported functions, e.g.
     * {@code "@Override\npublic boolean myMethod(int arg0)"}.
     */
    private final String exportMethodSignature;

    /**
     * Whether this exported method propagates events downstream (i.e. its dispatch list
     * includes @OnTrigger nodes beyond the root handler).
     * Based on the first callback's propagation flag for backward compatibility.
     */
    private final boolean propagates;

    /**
     * Per-callback propagation flags, parallel to {@link #callbackKeys}.
     * When non-empty, {@code callbackPropagates.get(i)} gives the propagation flag for
     * {@code callbackKeys.get(i)}.  May be empty for DTOs built before this field was added
     * — callers should fall back to {@link #propagates} in that case.
     */
    private final List<Boolean> callbackPropagates;

    /** Backward-compatible constructor (signature and propagates default to null/true). */
    public ExportFunctionDataDto(MethodDescriptor methodDescriptor, List<String> callbackKeys, boolean booleanReturn) {
        this(methodDescriptor, callbackKeys, booleanReturn, null, true, Collections.emptyList());
    }

    public ExportFunctionDataDto(MethodDescriptor methodDescriptor, List<String> callbackKeys,
                                  boolean booleanReturn, String exportMethodSignature, boolean propagates) {
        this(methodDescriptor, callbackKeys, booleanReturn, exportMethodSignature, propagates, Collections.emptyList());
    }

    public ExportFunctionDataDto(MethodDescriptor methodDescriptor, List<String> callbackKeys,
                                  boolean booleanReturn, String exportMethodSignature, boolean propagates,
                                  List<Boolean> callbackPropagates) {
        this.methodDescriptor = Objects.requireNonNull(methodDescriptor);
        this.callbackKeys = Collections.unmodifiableList(new ArrayList<>(callbackKeys));
        this.booleanReturn = booleanReturn;
        this.exportMethodSignature = exportMethodSignature;
        this.propagates = propagates;
        this.callbackPropagates = Collections.unmodifiableList(
                callbackPropagates != null ? new ArrayList<>(callbackPropagates) : new ArrayList<>());
    }

    public MethodDescriptor getMethodDescriptor() { return methodDescriptor; }
    public List<String> getCallbackKeys() { return callbackKeys; }
    public boolean isBooleanReturn() { return booleanReturn; }
    public String getExportMethodSignature() { return exportMethodSignature; }
    public boolean isPropagates() { return propagates; }
    /** Per-callback propagation flags parallel to {@link #callbackKeys}; may be empty for older DTOs. */
    public List<Boolean> getCallbackPropagates() { return callbackPropagates; }

    /**
     * Returns the propagation flag for a specific callback index.
     * Falls back to {@link #isPropagates()} when per-callback data is not available.
     */
    public boolean isPropagatesForIndex(int index) {
        if (callbackPropagates != null && index < callbackPropagates.size()) {
            return callbackPropagates.get(index);
        }
        return propagates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExportFunctionDataDto)) return false;
        ExportFunctionDataDto that = (ExportFunctionDataDto) o;
        return booleanReturn == that.booleanReturn
                && propagates == that.propagates
                && Objects.equals(methodDescriptor, that.methodDescriptor)
                && Objects.equals(callbackKeys, that.callbackKeys)
                && Objects.equals(exportMethodSignature, that.exportMethodSignature)
                && Objects.equals(callbackPropagates, that.callbackPropagates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodDescriptor, callbackKeys, booleanReturn);
    }

    @Override
    public String toString() {
        return methodDescriptor + " -> " + callbackKeys;
    }
}
