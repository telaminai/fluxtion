/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.builder.generation.annotationprocessor;

import com.google.auto.service.AutoService;
import com.telamin.fluxtion.runtime.annotations.AfterTrigger;
import com.telamin.fluxtion.runtime.annotations.ExportService;
import com.telamin.fluxtion.runtime.annotations.FluxtionDataOnly;
import com.telamin.fluxtion.runtime.annotations.Initialise;
import com.telamin.fluxtion.runtime.annotations.OnBatchEnd;
import com.telamin.fluxtion.runtime.annotations.OnBatchPause;
import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import com.telamin.fluxtion.runtime.annotations.OnParentUpdate;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.annotations.Start;
import com.telamin.fluxtion.runtime.annotations.Stop;
import com.telamin.fluxtion.runtime.annotations.TearDown;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Substrate Lint — emits a warning for classes that look node-shaped but
 * carry no Fluxtion trigger annotation. The class is silently passive at
 * runtime: the source compiles, the topology builds, and the missing
 * trigger only surfaces when downstream events go undelivered.
 *
 * <p>Suppress the warning by marking the class
 * {@link FluxtionDataOnly @FluxtionDataOnly} (intentional data class) or
 * by adding any of the trigger annotations listed in
 * {@link #TRIGGER_ANNOTATIONS}.
 *
 * <p>Heuristic mirrors {@code substrate-lint.md} Phase 2 but runs at
 * compile time:
 * <ol>
 *   <li>Top-level {@code class} (skip {@code record}, {@code interface},
 *       {@code enum}, abstract).</li>
 *   <li>Has at least one instance field (skip pure utility classes).</li>
 *   <li>Has no {@code public static void main(String[] args)} (skip
 *       launchers).</li>
 *   <li>No {@link FluxtionDataOnly} on the class.</li>
 *   <li>No method-level trigger annotation.</li>
 * </ol>
 *
 * <p>Severity is {@link Diagnostic.Kind#WARNING} — the source compiles
 * and is wired into the topology; the warning is the only signal that the
 * shape is suspicious.
 */
@AutoService(Processor.class)
public class ValidateMissingTriggerAnnotations extends AbstractProcessor {

    /**
     * Method-level annotations that cause a class to participate in the
     * Fluxtion event-dispatch graph. Mirrors
     * {@code TopologicallySortedDependencyGraph#annotationPredicate()}.
     */
    static final List<Class<? extends Annotation>> TRIGGER_ANNOTATIONS = Arrays.asList(
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
            if (!isCandidate(type)) {
                continue;
            }
            if (hasFluxtionDataOnly(type)) {
                continue;
            }
            if (hasAnyTriggerAnnotation(type)) {
                continue;
            }
            if (looksLikeImmutableValueObject(type)) {
                continue;
            }
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "[fluxtion-substrate-lint] class '" + type.getQualifiedName()
                            + "' has instance state but no Fluxtion trigger annotation"
                            + " (@OnEventHandler, @OnTrigger, @Initialise, @ExportService, ...)."
                            + " If it is registered as a node, events will not propagate. Add a"
                            + " trigger annotation, or mark the class @FluxtionDataOnly to silence"
                            + " this warning.",
                    type
            );
        }
        return false;
    }

    private boolean isCandidate(TypeElement type) {
        if (type.getKind() != ElementKind.CLASS) {
            return false;
        }
        if (type.getNestingKind() != NestingKind.TOP_LEVEL) {
            return false;
        }
        Set<Modifier> modifiers = type.getModifiers();
        if (modifiers.contains(Modifier.ABSTRACT)) {
            return false;
        }
        if (looksLikeTestClass(type)) {
            return false;
        }
        if (looksGenerated(type)) {
            return false;
        }
        boolean hasInstanceField = false;
        boolean hasMain = false;
        for (Element enclosed : type.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                if (!enclosed.getModifiers().contains(Modifier.STATIC)) {
                    hasInstanceField = true;
                }
            } else if (enclosed.getKind() == ElementKind.METHOD && isMainMethod((ExecutableElement) enclosed)) {
                hasMain = true;
            }
        }
        return hasInstanceField && !hasMain;
    }

    /**
     * Recognise test classes by JUnit method-level annotations and by the
     * conventional name suffixes used by surefire/failsafe. Test classes
     * typically have instance state (mocks, fixtures) but no Fluxtion
     * triggers — flagging them every build would train developers to
     * ignore the warning.
     */
    private boolean looksLikeTestClass(TypeElement type) {
        String simpleName = type.getSimpleName().toString();
        if (simpleName.endsWith("Test")
                || simpleName.endsWith("Tests")
                || simpleName.endsWith("TestCase")
                || simpleName.endsWith("IT")) {
            return true;
        }
        for (Element enclosed : type.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) {
                continue;
            }
            for (javax.lang.model.element.AnnotationMirror am : enclosed.getAnnotationMirrors()) {
                String fqn = am.getAnnotationType().toString();
                if (fqn.startsWith("org.junit.") || fqn.startsWith("junit.framework.")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isMainMethod(ExecutableElement method) {
        if (!method.getSimpleName().contentEquals("main")) {
            return false;
        }
        Set<Modifier> mods = method.getModifiers();
        if (!mods.contains(Modifier.PUBLIC) || !mods.contains(Modifier.STATIC)) {
            return false;
        }
        if (method.getReturnType().getKind() != TypeKind.VOID) {
            return false;
        }
        List<? extends VariableElement> params = method.getParameters();
        if (params.size() != 1) {
            return false;
        }
        TypeMirror paramType = params.get(0).asType();
        return paramType.getKind() == TypeKind.ARRAY
                && paramType.toString().equals("java.lang.String[]");
    }

    /**
     * Skip auto-generated source files. We don't have a path-level signal at
     * the Element level, so match on the {@code @Generated} marker by simple
     * name — covers {@code javax.annotation.Generated},
     * {@code javax.annotation.processing.Generated},
     * {@code jakarta.annotation.Generated} and {@code lombok.Generated}.
     */
    private boolean looksGenerated(TypeElement type) {
        for (javax.lang.model.element.AnnotationMirror am : type.getAnnotationMirrors()) {
            String simple = am.getAnnotationType().asElement().getSimpleName().toString();
            if ("Generated".equals(simple)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Immutable-value-object shape: at least one instance field, every
     * instance field is `final`. Fluxtion nodes generally hold mutable
     * runtime state (counters, accumulators, last-seen values), so an
     * all-final-fields class is almost always an event / payload / DTO
     * by convention (Trade, Tick, Quote, EnrichedTrade, Order). Silence
     * the warning without requiring an explicit `@FluxtionDataOnly` —
     * asking the user to annotate every event class is too noisy.
     */
    private boolean looksLikeImmutableValueObject(TypeElement type) {
        boolean hasInstanceField = false;
        for (Element enclosed : type.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.FIELD) {
                continue;
            }
            Set<Modifier> mods = enclosed.getModifiers();
            if (mods.contains(Modifier.STATIC)) {
                continue;
            }
            hasInstanceField = true;
            if (!mods.contains(Modifier.FINAL)) {
                return false;
            }
        }
        return hasInstanceField;
    }

    private boolean hasFluxtionDataOnly(TypeElement type) {
        return type.getAnnotation(FluxtionDataOnly.class) != null;
    }

    private boolean hasAnyTriggerAnnotation(TypeElement type) {
        // Class-level annotations (e.g., @ExportService can be applied to a type).
        for (Class<? extends Annotation> annClass : TRIGGER_ANNOTATIONS) {
            if (type.getAnnotation(annClass) != null) {
                return true;
            }
        }
        // Method-level annotations.
        for (Element enclosed : type.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) {
                continue;
            }
            for (Class<? extends Annotation> annClass : TRIGGER_ANNOTATIONS) {
                if (enclosed.getAnnotation(annClass) != null) {
                    return true;
                }
            }
        }
        // TYPE_USE annotations on `implements`/`extends` clauses.
        // @ExportService is declared @Target(TYPE_USE) and is conventionally
        // applied as `class Foo implements @ExportService Bar` — the
        // annotation rides on the superinterface type reference, not on the
        // class itself, so type.getAnnotation(...) above misses it.
        if (hasTypeUseTriggerOn(type.getSuperclass())) {
            return true;
        }
        for (TypeMirror iface : type.getInterfaces()) {
            if (hasTypeUseTriggerOn(iface)) {
                return true;
            }
        }
        // Source-level fallback. The TYPE_USE checks above rely on the
        // superinterface type resolving to a real `DeclaredType` — when it
        // resolves to an `ErrorType` (e.g. cross-file references in the
        // browser playground's CheerpJ compile, where sibling files don't
        // share a classpath at the moment the type-mirror walk runs), the
        // annotation on the type reference is not surfaced via the Element
        // model. Walking the parsed AST through the Trees API works
        // regardless of resolution status, and matches the user's mental
        // model: if a trigger annotation appears anywhere in the source,
        // the class is reactive.
        if (hasTriggerAnnotationInSource(type)) {
            return true;
        }
        return false;
    }

    private boolean hasTriggerAnnotationInSource(TypeElement type) {
        try {
            com.sun.source.util.Trees trees = com.sun.source.util.Trees.instance(processingEnv);
            com.sun.source.util.TreePath path = trees.getPath(type);
            if (path == null) {
                return false;
            }
            com.sun.source.tree.CompilationUnitTree cu = path.getCompilationUnit();
            if (cu == null) {
                return false;
            }
            String src = cu.toString();
            for (Class<? extends Annotation> annClass : TRIGGER_ANNOTATIONS) {
                String simple = "@" + annClass.getSimpleName();
                String fqn = "@" + annClass.getCanonicalName();
                if (src.contains(simple) || src.contains(fqn)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Trees may not be available in every environment (for instance,
            // some annotation-processor wrappers strip jdk.compiler access).
            // Fall back to the cheap structural checks already performed.
        }
        return false;
    }

    private boolean hasTypeUseTriggerOn(TypeMirror typeMirror) {
        if (typeMirror == null) {
            return false;
        }
        for (javax.lang.model.element.AnnotationMirror am : typeMirror.getAnnotationMirrors()) {
            String fqn = am.getAnnotationType().toString();
            for (Class<? extends Annotation> annClass : TRIGGER_ANNOTATIONS) {
                if (fqn.equals(annClass.getCanonicalName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        // Claim "*" so we run every round and can scan root elements
        // regardless of which annotations they carry. This is the only
        // way to flag classes that have NO trigger annotation at all.
        return new HashSet<>(Collections.singletonList("*"));
    }
}