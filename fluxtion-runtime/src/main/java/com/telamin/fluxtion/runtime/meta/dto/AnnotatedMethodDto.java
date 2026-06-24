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
 * Serializable description of an annotated method on a graph node.
 * Combines a {@link MethodDescriptor} (the method identity) with the list of
 * {@link AnnotationDescriptorDto} instances present on that method.
 * No live {@link java.lang.reflect.Method} or annotation references are held.
 */
public final class AnnotatedMethodDto implements Serializable {
    private static final long serialVersionUID = 2L;

    private final MethodDescriptor method;
    private final List<AnnotationDescriptorDto> annotations;
    /** Whether this method is declared {@code static}. Used to compute {@code methodTarget} in CbMethodHandle. */
    private final boolean isStatic;

    public AnnotatedMethodDto(MethodDescriptor method, List<AnnotationDescriptorDto> annotations) {
        this(method, annotations, false);
    }

    public AnnotatedMethodDto(MethodDescriptor method, List<AnnotationDescriptorDto> annotations, boolean isStatic) {
        this.method = Objects.requireNonNull(method);
        this.annotations = Collections.unmodifiableList(new ArrayList<>(annotations));
        this.isStatic = isStatic;
    }

    public MethodDescriptor getMethod() {
        return method;
    }

    public List<AnnotationDescriptorDto> getAnnotations() {
        return annotations;
    }

    public boolean isStatic() {
        return isStatic;
    }

    /** Return the first annotation whose class name equals {@code annotationClassName}, or {@code null}. */
    public AnnotationDescriptorDto findAnnotation(String annotationClassName) {
        for (AnnotationDescriptorDto a : annotations) {
            if (a.getAnnotationClassName().equals(annotationClassName)) return a;
        }
        return null;
    }

    /** Return all annotations whose class name equals {@code annotationClassName}. */
    public List<AnnotationDescriptorDto> findAllAnnotations(String annotationClassName) {
        List<AnnotationDescriptorDto> result = new ArrayList<>();
        for (AnnotationDescriptorDto a : annotations) {
            if (a.getAnnotationClassName().equals(annotationClassName)) result.add(a);
        }
        return result;
    }

    /** Return true if this method carries an annotation with the given class name. */
    public boolean hasAnnotation(String annotationClassName) {
        return findAnnotation(annotationClassName) != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnnotatedMethodDto)) return false;
        AnnotatedMethodDto that = (AnnotatedMethodDto) o;
        return isStatic == that.isStatic && Objects.equals(method, that.method) && Objects.equals(annotations, that.annotations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, annotations);
    }

    @Override
    public String toString() {
        return method + " " + annotations;
    }
}
