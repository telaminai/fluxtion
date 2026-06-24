/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.meta.model;

import com.telamin.fluxtion.runtime.meta.dto.MethodDescriptor;

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
    <T extends SourceCbMethodHandle> Map<String, Map<FilterDescription, List<T>>> getDispatchMap();
    <T extends SourceCbMethodHandle> Map<String, Map<FilterDescription, List<T>>> getPostDispatchMap();
    <T extends SourceCbMethodHandle> Map<String, Map<FilterDescription, List<T>>> getHandlerOnlyDispatchMap();
    <T extends SourceCbMethodHandle> List<T> getAllPostEventCallBacks();
    <T extends SourceCbMethodHandle> List<T> getTriggerOnlyCallBacks();
    Set<String> getForkedTriggerInstances();
    List<FilterDescription> getFilterDescriptionList();

    // dirty flag / guards
    Map<String, DirtyFlag> getDirtyFieldMap();
    DirtyFlag getDirtyFlagForUpdateCb(SourceCbMethodHandle cbHandle);
    List<String> sortByClassHierarchy(Collection<String> classSet);
    Collection<DirtyFlag> getNodeGuardConditions(String nodeName);
    Collection<DirtyFlag> getNodeGuardConditions(SourceCbMethodHandle cbHandle);

    // parent listeners
    <T extends SourceCbMethodHandle> Map<String, List<T>> getParentUpdateListenerMethodMap();

    // utilities used in generation
    String getMappedClass(String className);
    String getCanonicalName(String className);
    String getTypeDeclaration(String variableName);
    String constructorString(String fieldName);
    List<String> beanProperties(String field);
    List<String> publicProperties(String field);
    Set<String> getImportClasses();

    // field lookup by name (for getNodeById in InMemoryEventProcessor)
    SourceField getFieldForName(String name);

    // topologically ordered callback list for dispatch
    <T extends SourceCbMethodHandle> List<T> getDispatchMapForGraph();

    // OnTrigger dependency keys for a callback (key format: "variableName::methodName")
    List<String> getOnTriggerDependentCallbackKeys(SourceCbMethodHandle cbHandle);

    // exported function map for dynamic proxy generation
    Map<MethodDescriptor, ExportFunctionData> getExportedFunctionMap();

    // graph visualisation outputs (populated server-side during generate())
    default String getGraphMlOutput() { return null; }
    default byte[] getPngOutput() { return null; }

    /**
     * Populates live instances and Method references after DTO-based model generation.
     * Called client-side before constructing InMemoryEventProcessor.
     * Default no-op for implementations that don't need instance resolution.
     */
    default void resolveInstances(Map<String, Object> instanceMap) {}
}
