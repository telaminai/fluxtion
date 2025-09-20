/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.generation.annotationprocessor;

import com.fluxtion.dataflow.runtime.annotations.*;
import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(Processor.class)
public class ValidateLifecycleAnnotations extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            Set<? extends Element> typeElements = annotatedElements.stream()
                    .filter(element ->
                            ((ExecutableType) element.asType()).getParameterTypes().size() != 0
                                    || !element.getModifiers().contains(Modifier.PUBLIC)
                    )
                    .collect(Collectors.toSet());

            typeElements.forEach(element ->
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "should be public method "
                                    + "with no arguments failing method:" + ((ExecutableElement) element).getSimpleName(), element));
        }
        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> supportedAnnotations = new HashSet<>();
        supportedAnnotations.add(AfterEvent.class.getCanonicalName());
        supportedAnnotations.add(AfterTrigger.class.getCanonicalName());
        supportedAnnotations.add(Initialise.class.getCanonicalName());
        supportedAnnotations.add(OnBatchEnd.class.getCanonicalName());
        supportedAnnotations.add(OnBatchPause.class.getCanonicalName());
        supportedAnnotations.add(OnTrigger.class.getCanonicalName());
        supportedAnnotations.add(TearDown.class.getCanonicalName());
        return supportedAnnotations;
    }
}
