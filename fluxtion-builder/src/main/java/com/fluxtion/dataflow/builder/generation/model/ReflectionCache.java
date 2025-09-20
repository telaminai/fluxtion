/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.fluxtion.dataflow.builder.generation.model;

import com.fluxtion.dataflow.builder.generation.util.ClassUtils;
import com.fluxtion.dataflow.runtime.DataFlow;
import com.fluxtion.dataflow.runtime.annotations.*;
import com.fluxtion.dataflow.runtime.annotations.builder.Inject;
import com.fluxtion.dataflow.runtime.annotations.builder.SepNode;
import com.fluxtion.dataflow.runtime.annotations.OnEventHandler;
import com.fluxtion.dataflow.runtime.annotations.OnTrigger;
import com.fluxtion.dataflow.runtime.annotations.TriggerEventOverride;
import com.fluxtion.dataflow.runtime.flowfunction.Tuple;
import com.fluxtion.dataflow.runtime.node.EventHandlerNode;
import org.reflections.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class ReflectionCache {

    private final Map<Class<?>, Set<Field>> fieldCache = new java.util.HashMap<>();
    private final Map<Tuple<Class<?>, Class<?>>, Set<Field>> fieldPairCache = new java.util.HashMap<>();
    private final Map<Object, Boolean> handleEventCache = new java.util.HashMap<>();
    private final Predicate<AnnotatedElement> predicate = ReflectionUtils.withAnnotation(OnEventHandler.class)
            .or(ReflectionUtils.withAnnotation(OnTrigger.class))
            .or(ReflectionUtils.withAnnotation(TriggerEventOverride.class));

    private final Map<Class<?>, Boolean> handleAnyCallbackCache = new java.util.HashMap<>();
    private final Predicate<AnnotatedElement> anyHandlerPredicate = ReflectionUtils.withAnnotation(AfterEvent.class)
            .or(ReflectionUtils.withAnnotation(OnEventHandler.class))
            .or(ReflectionUtils.withAnnotation(Inject.class))
            .or(ReflectionUtils.withAnnotation(OnBatchEnd.class))
            .or(ReflectionUtils.withAnnotation(OnBatchPause.class))
            .or(ReflectionUtils.withAnnotation(OnTrigger.class))
            .or(ReflectionUtils.withAnnotation(AfterTrigger.class))
            .or(ReflectionUtils.withAnnotation(OnParentUpdate.class))
            .or(ReflectionUtils.withAnnotation(TearDown.class))
            .or(ReflectionUtils.withAnnotation(Initialise.class))
            .or(ReflectionUtils.withAnnotation(TriggerEventOverride.class))
            .or(ReflectionUtils.withAnnotation(ExportService.class));

    private final Map<Class<?>, Boolean> eventHandlerClassMap = new java.util.HashMap<>();

    public Set<Field> getAllFields(Class<?> clazz) {
        if (clazz == null) {
            return new HashSet<>();
        } else {
            if (fieldCache.containsKey(clazz)) {
                return fieldCache.get(clazz);
            } else {
                Set<Field> fields = ReflectionUtils.getAllFields(clazz);
                fieldCache.put(clazz, fields);
                return fields;
            }
        }
    }

    public Set<Field> getAllFields(Class<?> clazz, final Class<? extends Annotation> annotation) {
        if (clazz == null | annotation == null) {
            return new HashSet<>();
        } else {
            Tuple<Class<?>, Class<?>> key = Tuple.build(clazz, annotation);
            if (fieldPairCache.containsKey(key)) {
                return fieldPairCache.get(key);
            } else {
                Set<Field> fields = ReflectionUtils.getAllFields(clazz, ReflectionUtils.withAnnotation(annotation));
                fieldPairCache.put(key, fields);
                return fields;
            }
        }
    }

    public boolean isHandleEvent(Object object) {
        Class<?> clazz = object.getClass();
        if (handleEventCache.containsKey(clazz)) {
            return handleEventCache.get(clazz);
        } else {

            boolean handlesEvents = EventHandlerNode.class.isAssignableFrom(clazz)
                    || DataFlow.class.isAssignableFrom(clazz)
                    || isEventHandlerClass(clazz)
                    || ClassUtils.isPropagatingExportService(clazz);
            handleEventCache.put(clazz, handlesEvents);
            return handlesEvents;
        }
    }

    public boolean isHandler(Object refField) {

        Class<?> clazz = refField.getClass();
        if (handleAnyCallbackCache.containsKey(clazz)) {
            return handleEventCache.get(clazz);
        } else {

            boolean addNode = !ReflectionUtils.getAllMethods(
                    refField.getClass(),
                    anyHandlerPredicate
            ).isEmpty();
            addNode |= EventHandlerNode.class.isAssignableFrom(refField.getClass())
                    | DataFlow.class.isAssignableFrom(refField.getClass())
                    | refField.getClass().getAnnotation(SepNode.class) != null
                    | !ClassUtils.getAllAnnotatedAnnotationTypes(refField.getClass(), ExportService.class).isEmpty()
            ;
            handleEventCache.put(clazz, addNode);
            return addNode;
        }
    }

    private boolean isEventHandlerClass(Class<?> clazz) {
        if(eventHandlerClassMap.containsKey(clazz)){
            return eventHandlerClassMap.get(clazz);
        }else{
            boolean hasEventMethod = !ReflectionUtils.getAllMethods(clazz, predicate).isEmpty();
            eventHandlerClassMap.put(clazz, hasEventMethod);
            return hasEventMethod;
        }
    }

}
