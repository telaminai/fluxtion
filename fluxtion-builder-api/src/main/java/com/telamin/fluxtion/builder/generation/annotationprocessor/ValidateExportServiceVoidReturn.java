/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.builder.generation.annotationprocessor;

import com.google.auto.service.AutoService;
import com.telamin.fluxtion.runtime.annotations.ExportService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Substrate Lint rule — flags methods on {@code @ExportService}-annotated
 * elements (interfaces or implementing classes) whose return type is not
 * {@code void}.
 *
 * <p>Fluxtion treats exported services as command surfaces: each exported
 * method is dispatched into the graph and any return value is silently
 * discarded. A non-void return signature compiles fine and runs at
 * call-time, but the caller's read of the return value is always the
 * default ({@code 0} / {@code null} / {@code false}). Users hit this
 * after writing an exported {@code int countOrders()}-style query and
 * wondering why they always see zero — exactly the silent-misbehaviour
 * pattern the Substrate Lint surface targets. For queries, use
 * {@code flow.getNodeById(name)}.
 *
 * <p>Severity is {@link Diagnostic.Kind#WARNING} for consistency with
 * the rest of the substrate-lint surface.
 */
@AutoService(Processor.class)
public class ValidateExportServiceVoidReturn extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        for (Element root : roundEnv.getRootElements()) {
            if (!(root instanceof TypeElement)) continue;
            TypeElement type = (TypeElement) root;
            if (!isExportService(type)) continue;
            for (Element enclosed : type.getEnclosedElements()) {
                if (enclosed.getKind() != ElementKind.METHOD) continue;
                ExecutableElement method = (ExecutableElement) enclosed;
                if (skipMethod(method)) continue;
                TypeMirror returnType = method.getReturnType();
                if (returnType.getKind() == TypeKind.VOID) continue;
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.WARNING,
                        "[fluxtion-substrate-lint] @ExportService method '"
                                + type.getQualifiedName()
                                + "#" + method.getSimpleName()
                                + "' returns " + returnType
                                + ". Exported services are command surfaces — Fluxtion"
                                + " dispatches the call into the graph and discards any"
                                + " return value, so the caller always sees the default."
                                + " Change the return type to void, or use"
                                + " flow.getNodeById(\"name\") for query access.",
                        method
                );
            }
        }
        return false;
    }

    /**
     * Scope: class-level {@code @ExportService} annotation only. TYPE_USE
     * {@code @ExportService(...)} on {@code implements} / {@code extends}
     * clauses is *also* a real export pattern, but flagging "every public
     * method of the impl class" there overreaches — only the methods
     * declared by the exported interface are part of the dispatch surface,
     * and impl classes routinely have additional public helpers that are
     * not. Enforcing the rule for TYPE_USE cases would need interface-method
     * resolution which is fragile under CheerpJ where cross-file types
     * resolve as ErrorType. Keep the rule conservative: class-level only.
     * TYPE_USE coverage is a follow-up that walks
     * {@code type.getInterfaces()} and only checks methods that override
     * the exported interface's methods.
     */
    private boolean isExportService(TypeElement type) {
        return type.getAnnotation(ExportService.class) != null;
    }

    /**
     * Skip private / static / synthetic methods and constructors —
     * exported-service contract only applies to instance public methods
     * (the dispatch surface). Static factories on an exported class are
     * fine; they aren't dispatched by Fluxtion.
     */
    private boolean skipMethod(ExecutableElement method) {
        if (method.getModifiers().contains(Modifier.STATIC)) return true;
        if (method.getModifiers().contains(Modifier.PRIVATE)) return true;
        // Synthetic / bridge methods don't have source-level positions;
        // their warnings would point at noise. Skip.
        if (method.getSimpleName().toString().startsWith("<")) return true;
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
