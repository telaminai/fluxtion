/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.builder.generation.annotationprocessor;

import com.google.auto.service.AutoService;
import com.telamin.fluxtion.runtime.annotations.AfterTrigger;
import com.telamin.fluxtion.runtime.annotations.ExportService;
import com.telamin.fluxtion.runtime.annotations.Initialise;
import com.telamin.fluxtion.runtime.annotations.OnBatchEnd;
import com.telamin.fluxtion.runtime.annotations.OnBatchPause;
import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import com.telamin.fluxtion.runtime.annotations.OnParentUpdate;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.annotations.Start;
import com.telamin.fluxtion.runtime.annotations.Stop;
import com.telamin.fluxtion.runtime.annotations.TearDown;
import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Substrate Lint rule — flags constructors with multiple parameters of the
 * same type where {@link AssignToField} is missing on at least one of them.
 *
 * <p>Fluxtion source-gen binds constructor parameters to fields by type at
 * generation time. When two parameters share a type and neither carries an
 * explicit {@code @AssignToField("fieldName")} hint, source-gen cannot
 * decide which parameter goes to which field — generation fails with an
 * unhelpful error that stumps developers on their first ambiguous-ctor
 * node. This warning surfaces the problem at javac time, before the
 * developer has waited through a build, with a clear hint to add
 * {@code @AssignToField} on each ambiguous parameter.
 *
 * <p>Gated on plausibly-node classes (any class carrying any trigger
 * annotation) so pure data classes with multi-arg ctors (Trade, Quote)
 * stay silent — those don't go through Fluxtion source-gen.
 *
 * <p>Severity is {@link Diagnostic.Kind#WARNING} for consistency with
 * the rest of the substrate-lint surface.
 */
@AutoService(Processor.class)
public class ValidateMissingAssignToField extends AbstractProcessor {

    /**
     * Method-level annotations that mark a class as a Fluxtion node. Only
     * classes carrying at least one of these are checked — pure data
     * classes don't go through source-gen so the rule doesn't apply.
     */
    static final List<Class<? extends Annotation>> NODE_MARKER_ANNOTATIONS = Arrays.asList(
            OnEventHandler.class,
            OnTrigger.class,
            OnParentUpdate.class,
            AfterTrigger.class,
            Initialise.class,
            Start.class,
            Stop.class,
            TearDown.class,
            OnBatchPause.class,
            OnBatchEnd.class,
            ExportService.class
    );

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        for (Element root : roundEnv.getRootElements()) {
            if (!(root instanceof TypeElement)) {
                continue;
            }
            TypeElement type = (TypeElement) root;
            if (type.getKind() != ElementKind.CLASS) {
                continue;
            }
            if (!isPlausiblyNode(type)) {
                continue;
            }
            for (Element enclosed : type.getEnclosedElements()) {
                if (enclosed.getKind() != ElementKind.CONSTRUCTOR) {
                    continue;
                }
                checkConstructor(type, (ExecutableElement) enclosed);
            }
        }
        return false;
    }

    private void checkConstructor(TypeElement type, ExecutableElement ctor) {
        List<? extends VariableElement> params = ctor.getParameters();
        if (params.size() < 2) {
            return;
        }
        // Group by string-form type — TypeMirror.equals() isn't reliable for
        // grouping; the canonical toString() is. Linked map preserves source
        // order so the diagnostic message names types in declaration order.
        Map<String, List<VariableElement>> byType = new LinkedHashMap<>();
        for (VariableElement p : params) {
            String key = p.asType().toString();
            byType.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(p);
        }
        for (Map.Entry<String, List<VariableElement>> entry : byType.entrySet()) {
            List<VariableElement> group = entry.getValue();
            if (group.size() < 2) {
                continue;
            }
            boolean allMarked = true;
            for (VariableElement p : group) {
                if (p.getAnnotation(AssignToField.class) == null) {
                    allMarked = false;
                    break;
                }
            }
            if (allMarked) {
                continue;
            }
            StringBuilder names = new StringBuilder();
            for (int i = 0; i < group.size(); i++) {
                if (i > 0) names.append(", ");
                names.append(group.get(i).getSimpleName());
            }
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "[fluxtion-substrate-lint] constructor of '"
                            + type.getQualifiedName()
                            + "' has multiple parameters of type '" + entry.getKey()
                            + "' (" + names + ") without @AssignToField. Fluxtion source-gen"
                            + " cannot decide which parameter binds to which field. Mark each"
                            + " ambiguous parameter @AssignToField(\"<fieldName>\").",
                    ctor
            );
            // One warning per constructor is enough — naming every duplicate
            // type group would multiply noise. The dev sees the first; once
            // they fix it the rest surface on the next compile.
            return;
        }
    }

    private boolean isPlausiblyNode(TypeElement type) {
        for (Class<? extends Annotation> annClass : NODE_MARKER_ANNOTATIONS) {
            if (type.getAnnotation(annClass) != null) {
                return true;
            }
        }
        for (Element enclosed : type.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) {
                continue;
            }
            for (Class<? extends Annotation> annClass : NODE_MARKER_ANNOTATIONS) {
                if (enclosed.getAnnotation(annClass) != null) {
                    return true;
                }
            }
        }
        // Source-level fallback for environments where TYPE_USE annotations
        // on superinterfaces resolve to ErrorType (e.g. CheerpJ playground
        // compile). Same approach as ValidateMissingTriggerAnnotations.
        try {
            com.sun.source.util.Trees trees = com.sun.source.util.Trees.instance(processingEnv);
            com.sun.source.util.TreePath path = trees.getPath(type);
            if (path != null && path.getCompilationUnit() != null) {
                String src = path.getCompilationUnit().toString();
                for (Class<? extends Annotation> annClass : NODE_MARKER_ANNOTATIONS) {
                    if (src.contains("@" + annClass.getSimpleName())) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
            // Trees not available — skip the source-level fallback.
        }
        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<>(Collections.singletonList("*"));
    }
}
