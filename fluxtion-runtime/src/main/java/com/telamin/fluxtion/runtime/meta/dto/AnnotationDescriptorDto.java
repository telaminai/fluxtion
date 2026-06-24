/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.dto;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Serializable description of an annotation present on a method or field.
 * Stores the annotation class name and a flat map of attribute name → value.
 * Only primitive wrapper types, {@link String}, and arrays thereof are stored as values
 * so this DTO never holds live class references.
 */
public final class AnnotationDescriptorDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Canonical class name of the annotation, e.g. {@code "com.telamin.fluxtion.runtime.annotations.OnEventHandler"}. */
    private final String annotationClassName;

    /**
     * Annotation attribute values keyed by attribute name.
     * Permitted value types: {@link String}, {@link Integer}, {@link Long},
     * {@link Double}, {@link Float}, {@link Boolean}, {@link Character},
     * {@code int[]}, {@code String[]}.
     */
    private final Map<String, Object> attributes;

    public AnnotationDescriptorDto(String annotationClassName, Map<String, Object> attributes) {
        this.annotationClassName = Objects.requireNonNull(annotationClassName);
        this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    public AnnotationDescriptorDto(String annotationClassName) {
        this(annotationClassName, Collections.emptyMap());
    }

    public String getAnnotationClassName() {
        return annotationClassName;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /** Convenience: return the attribute value for {@code key}, or {@code defaultValue} if absent. */
    public Object getAttribute(String key, Object defaultValue) {
        return attributes.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnnotationDescriptorDto)) return false;
        AnnotationDescriptorDto that = (AnnotationDescriptorDto) o;
        return Objects.equals(annotationClassName, that.annotationClassName)
                && Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(annotationClassName, attributes);
    }

    @Override
    public String toString() {
        return "@" + annotationClassName + (attributes.isEmpty() ? "" : attributes.toString());
    }
}
