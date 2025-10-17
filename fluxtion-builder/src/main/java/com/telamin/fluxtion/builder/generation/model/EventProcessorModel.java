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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Minimal interface exposing only the methods required by JavaSourceGenerator.
 */
public interface EventProcessorModel {

    // lifecycle callback accessors
    List<CbMethodHandle> getInitialiseMethods();
    List<CbMethodHandle> getStartMethods();
    List<CbMethodHandle> getStartCompleteMethods();
    List<CbMethodHandle> getStopMethods();
    List<CbMethodHandle> getBatchPauseMethods();
    List<CbMethodHandle> getEventEndMethods();
    List<CbMethodHandle> getBatchEndMethods();
    List<CbMethodHandle> getTearDownMethods();

    // configuration flags
    boolean isDispatchOnlyVersion();

    // node and field accessors
    <T extends SourceField> List<T> getNodeFields();
    <T extends SourceField> List<T> getTopologicallySortedNodeFields();
    <T extends SourceField> List<T> getNodeRegistrationListenerFields();

    // dispatch and filtering
    Map<Class<?>, Map<FilterDescription, List<CbMethodHandle>>> getDispatchMap();
    Map<Class<?>, Map<FilterDescription, List<CbMethodHandle>>> getPostDispatchMap();
    Map<Class<?>, Map<FilterDescription, List<CbMethodHandle>>> getHandlerOnlyDispatchMap();
    List<CbMethodHandle> getAllPostEventCallBacks();
    List<CbMethodHandle> getTriggerOnlyCallBacks();
    Set<String> getForkedTriggerInstances();
    List<FilterDescription> getFilterDescriptionList();

    // dirty flag / guards
    Map<String, DirtyFlag> getDirtyFieldMap();
    DirtyFlag getDirtyFlagForUpdateCb(CbMethodHandle cbHandle);
    Collection<DirtyFlag> getNodeGuardConditions(String nodeName);
    Collection<DirtyFlag> getNodeGuardConditions(CbMethodHandle cbHandle);

    // parent listeners
    Map<String, List<CbMethodHandle>> getParentUpdateListenerMethodMap();

    // utilities used in generation
    String getMappedClass(String className);
    String getTypeDeclaration(String variableName);
    String constructorString(String fieldName);
    List<String> beanProperties(String field);
    List<String> publicProperties(String field);
    String getNameForInstance(Object object);
    Set<Class<?>> getImportClasses();
}
