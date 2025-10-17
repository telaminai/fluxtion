/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.generation.model;

import com.telamin.fluxtion.builder.filter.FilterDescription;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Minimal interface exposing only the methods required by JavaSourceGenerator.
 */
public interface EventProcessorModel extends Serializable {

    // lifecycle callback accessors
    <T extends SourceCbMethodHandle> List<T> getInitialiseMethods();
    <T extends SourceCbMethodHandle> List<T> getStartMethods();
    <T extends SourceCbMethodHandle> List<T> getStartCompleteMethods();
    <T extends SourceCbMethodHandle> List<T> getStopMethods();
    <T extends SourceCbMethodHandle> List<T> getBatchPauseMethods();
    <T extends SourceCbMethodHandle> List<T> getEventEndMethods();
    <T extends SourceCbMethodHandle> List<T> getBatchEndMethods();
    <T extends SourceCbMethodHandle> List<T> getTearDownMethods();

    // configuration flags
    boolean isDispatchOnlyVersion();

    // node and field accessors
    <T extends SourceField> List<T> getNodeFields();
    <T extends SourceField> List<T> getTopologicallySortedNodeFields();
    <T extends SourceField> List<T> getNodeRegistrationListenerFields();

    // dispatch and filtering
    <T extends SourceCbMethodHandle> Map<Class<?>, Map<FilterDescription, List<T>>> getDispatchMap();
    <T extends SourceCbMethodHandle> Map<Class<?>, Map<FilterDescription, List<T>>> getPostDispatchMap();
    <T extends SourceCbMethodHandle> Map<Class<?>, Map<FilterDescription, List<T>>> getHandlerOnlyDispatchMap();
    <T extends SourceCbMethodHandle> List<T> getAllPostEventCallBacks();
    <T extends SourceCbMethodHandle> List<T> getTriggerOnlyCallBacks();
    Set<String> getForkedTriggerInstances();
    List<FilterDescription> getFilterDescriptionList();

    // dirty flag / guards
    Map<String, DirtyFlag> getDirtyFieldMap();
    DirtyFlag getDirtyFlagForUpdateCb(SourceCbMethodHandle cbHandle);
    Collection<DirtyFlag> getNodeGuardConditions(String nodeName);
    Collection<DirtyFlag> getNodeGuardConditions(SourceCbMethodHandle cbHandle);

    // parent listeners
    <T extends SourceCbMethodHandle> Map<String, List<T>> getParentUpdateListenerMethodMap();

    // utilities used in generation
    String getMappedClass(String className);
    String getTypeDeclaration(String variableName);
    String constructorString(String fieldName);
    List<String> beanProperties(String field);
    List<String> publicProperties(String field);
    String getNameForInstance(Object object);
    Set<Class<?>> getImportClasses();
}
