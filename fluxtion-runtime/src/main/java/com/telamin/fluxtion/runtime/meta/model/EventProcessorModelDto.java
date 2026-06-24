/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.model;

import com.telamin.fluxtion.runtime.meta.dto.MethodDescriptor;

import java.io.Serializable;
import java.util.*;

/**
 * A pure-data transfer object implementing {@link EventProcessorModel} that can be safely
 * serialized between a generation provider and the client. Unlike
 * {@code SimpleEventProcessorModel}, this DTO resides in {@code fluxtion-runtime} and is therefore
 * available on the client classpath.
 *
 * <p>The server converts a {@code SimpleEventProcessorModel} into this DTO before serializing
 * the response. The client deserializes it and can then use it directly with
 * {@code JavaSourceGenerator} or pass it to {@code InMemoryEventProcessor} (after calling
 * {@link #resolveInstances(Map)} to populate live object references).
 *
 * <p>All fields use only types from {@code fluxtion-runtime}: {@link CbMethodHandle},
 * {@link Field}, {@link DirtyFlag}, {@link FilterDescription}, and {@link ExportFunctionData}.
 */
public class EventProcessorModelDto implements EventProcessorModel, Serializable {
    private static final long serialVersionUID = 1L;

    // ── lifecycle callback lists ────────────────────────────────────────
    private List<CbMethodHandle> initialiseMethods = new ArrayList<>();
    private List<CbMethodHandle> startMethods = new ArrayList<>();
    private List<CbMethodHandle> startCompleteMethods = new ArrayList<>();
    private List<CbMethodHandle> stopMethods = new ArrayList<>();
    private List<CbMethodHandle> batchPauseMethods = new ArrayList<>();
    private List<CbMethodHandle> eventEndMethods = new ArrayList<>();
    private List<CbMethodHandle> batchEndMethods = new ArrayList<>();
    private List<CbMethodHandle> tearDownMethods = new ArrayList<>();

    // ── configuration flags ─────────────────────────────────────────────
    private boolean dispatchOnlyVersion;

    // ── node / field lists ──────────────────────────────────────────────
    private List<Field> nodeFields = new ArrayList<>();
    private List<Field> topologicallySortedNodeFields = new ArrayList<>();
    private List<Field> nodeRegistrationListenerFields = new ArrayList<>();

    // ── dispatch maps ───────────────────────────────────────────────────
    private Map<String, Map<FilterDescription, List<CbMethodHandle>>> dispatchMap = new LinkedHashMap<>();
    private Map<String, Map<FilterDescription, List<CbMethodHandle>>> postDispatchMap = new LinkedHashMap<>();
    private Map<String, Map<FilterDescription, List<CbMethodHandle>>> handlerOnlyDispatchMap = new LinkedHashMap<>();
    private List<CbMethodHandle> allEventCallBacks = new ArrayList<>();
    private List<CbMethodHandle> allPostEventCallBacks = new ArrayList<>();
    private List<CbMethodHandle> triggerOnlyCallBacks;
    private Set<String> forkedTriggerInstances;
    private List<FilterDescription> filterDescriptionList = new ArrayList<>();

    // ── dirty flags / guards ────────────────────────────────────────────
    private Map<String, DirtyFlag> dirtyFieldMap = new LinkedHashMap<>();
    private Map<String, List<DirtyFlag>> nodeGuardMap = new LinkedHashMap<>();
    private boolean supportDirtyFiltering;

    // ── parent listeners ────────────────────────────────────────────────
    private Map<String, List<CbMethodHandle>> parentUpdateListenerMethodMap = new LinkedHashMap<>();
    private Map<String, List<String>> directParentMap = new LinkedHashMap<>();

    // ── class metadata ──────────────────────────────────────────────────
    private Map<String, String> class2CanonicalNameMap = new LinkedHashMap<>();
    private List<String> hierarchySortedClassList = new ArrayList<>();
    private Set<String> importClasses = new LinkedHashSet<>();
    private Map<String, String> class2ReplaceMap = new LinkedHashMap<>();

    // ── constructor / property maps ─────────────────────────────────────
    private Map<String, String> constructorStringMap = new LinkedHashMap<>();
    private Map<String, List<String>> beanPropertyMap = new LinkedHashMap<>();
    private Map<String, List<String>> publicMemberMap = new LinkedHashMap<>();
    private Map<String, String> typeMap = new LinkedHashMap<>();

    // ── on-trigger dependency keys ──────────────────────────────────────
    private Map<String, List<String>> onTriggerDependentCallbackKeys = new LinkedHashMap<>();

    // ── exported function map ───────────────────────────────────────────
    private Map<MethodDescriptor, ExportFunctionData> exportedFunctionMap = new LinkedHashMap<>();

    // ── graph visualisation ─────────────────────────────────────────────
    private String graphMlOutput;
    private byte[] pngOutput;

    // ═══════════════════════════════════════════════════════════════════
    // EventProcessorModel interface implementation
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SourceCbMethodHandle> List<T> getInitialiseMethods() {
        return (List<T>) Collections.unmodifiableList(initialiseMethods);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SourceCbMethodHandle> List<T> getStartMethods() {
        return (List<T>) Collections.unmodifiableList(startMethods);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SourceCbMethodHandle> List<T> getStartCompleteMethods() {
        return (List<T>) Collections.unmodifiableList(startCompleteMethods);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SourceCbMethodHandle> List<T> getStopMethods() {
        return (List<T>) Collections.unmodifiableList(stopMethods);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SourceCbMethodHandle> List<T> getBatchPauseMethods() {
        return (List<T>) Collections.unmodifiableList(batchPauseMethods);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SourceCbMethodHandle> List<T> getEventEndMethods() {
        return (List<T>) Collections.unmodifiableList(eventEndMethods);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SourceCbMethodHandle> List<T> getBatchEndMethods() {
        return (List<T>) Collections.unmodifiableList(batchEndMethods);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SourceCbMethodHandle> List<T> getTearDownMethods() {
        return (List<T>) Collections.unmodifiableList(tearDownMethods);
    }

    @Override
    public boolean isDispatchOnlyVersion() {
        return dispatchOnlyVersion;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SourceField> List<T> getNodeFields() {
        return (List<T>) Collections.unmodifiableList(nodeFields);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SourceField> List<T> getTopologicallySortedNodeFields() {
        return (List<T>) Collections.unmodifiableList(topologicallySortedNodeFields);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SourceField> List<T> getNodeRegistrationListenerFields() {
        return (List<T>) Collections.unmodifiableList(nodeRegistrationListenerFields);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SourceCbMethodHandle> Map<String, Map<FilterDescription, List<T>>> getDispatchMap() {
        return (Map<String, Map<FilterDescription, List<T>>>) (Map<?, ?>) Collections.unmodifiableMap(dispatchMap);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SourceCbMethodHandle> Map<String, Map<FilterDescription, List<T>>> getPostDispatchMap() {
        return (Map<String, Map<FilterDescription, List<T>>>) (Map<?, ?>) Collections.unmodifiableMap(postDispatchMap);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SourceCbMethodHandle> Map<String, Map<FilterDescription, List<T>>> getHandlerOnlyDispatchMap() {
        return (Map<String, Map<FilterDescription, List<T>>>) (Map<?, ?>) Collections.unmodifiableMap(handlerOnlyDispatchMap);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SourceCbMethodHandle> List<T> getAllPostEventCallBacks() {
        return (List<T>) Collections.unmodifiableList(allPostEventCallBacks);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SourceCbMethodHandle> List<T> getTriggerOnlyCallBacks() {
        if (triggerOnlyCallBacks == null) {
            triggerOnlyCallBacks = new ArrayList<>();
            for (CbMethodHandle cb : allEventCallBacks) {
                if (!(cb.isEventHandler() || cb.isNoPropagateEventHandler())) {
                    triggerOnlyCallBacks.add(cb);
                }
            }
            triggerOnlyCallBacks = Collections.unmodifiableList(triggerOnlyCallBacks);
        }
        return (List<T>) triggerOnlyCallBacks;
    }

    @Override
    public Set<String> getForkedTriggerInstances() {
        if (forkedTriggerInstances == null) {
            Set<String> set = new LinkedHashSet<>();
            for (SourceCbMethodHandle cb : this.<SourceCbMethodHandle>getTriggerOnlyCallBacks()) {
                if (cb.isForkExecution()) {
                    set.add(cb.getVariableName());
                }
            }
            forkedTriggerInstances = Collections.unmodifiableSet(set);
        }
        return forkedTriggerInstances;
    }

    @Override
    public List<FilterDescription> getFilterDescriptionList() {
        return Collections.unmodifiableList(filterDescriptionList);
    }

    @Override
    public Map<String, DirtyFlag> getDirtyFieldMap() {
        return Collections.unmodifiableMap(dirtyFieldMap);
    }

    @Override
    public DirtyFlag getDirtyFlagForUpdateCb(SourceCbMethodHandle cbHandle) {
        DirtyFlag flag = null;
        if (supportDirtyFiltering && cbHandle != null) {
            flag = dirtyFieldMap.get(cbHandle.getVariableName());
            if (flag != null && !cbHandle.getReturnType().equalsIgnoreCase(boolean.class.getCanonicalName())) {
                flag.alwaysDirty = true;
            }
        }
        return flag;
    }

    private static final NaturalOrderComparator<?> COMPARATOR = new NaturalOrderComparator<>();

    @Override
    public List<String> sortByClassHierarchy(Collection<String> classSet) {
        List<String> sorted = new ArrayList<>();
        for (String clazz : hierarchySortedClassList) {
            if (classSet.contains(clazz)) {
                sorted.add(clazz);
            }
        }
        return sorted;
    }

    @Override
    public Collection<DirtyFlag> getNodeGuardConditions(String nodeName) {
        List<DirtyFlag> guards = nodeGuardMap.getOrDefault(nodeName, Collections.emptyList());
        List<DirtyFlag> sorted = new ArrayList<>(guards);
        sorted.sort((o1, o2) -> COMPARATOR.compare(o1.name, o2.name));
        return sorted;
    }

    @Override
    public Collection<DirtyFlag> getNodeGuardConditions(SourceCbMethodHandle cb) {
        if (cb.isPostEventHandler()
                && directParentMap.getOrDefault(cb.getVariableName(), Collections.emptyList()).isEmpty()) {
            DirtyFlag flag = dirtyFieldMap.get(cb.getVariableName());
            return flag == null ? Collections.emptyList() : Collections.singletonList(flag);
        }
        return cb.isEventHandler() ? Collections.emptySet() : getNodeGuardConditions(cb.getVariableName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SourceCbMethodHandle> Map<String, List<T>> getParentUpdateListenerMethodMap() {
        return (Map<String, List<T>>) (Map<?, ?>) Collections.unmodifiableMap(parentUpdateListenerMethodMap);
    }

    @Override
    public String getMappedClass(String className) {
        return class2ReplaceMap.getOrDefault(className, className);
    }

    @Override
    public String getCanonicalName(String className) {
        return class2CanonicalNameMap.getOrDefault(className, className).replace('$', '.');
    }

    @Override
    public String getTypeDeclaration(String variableName) {
        return typeMap.getOrDefault(variableName, "");
    }

    @Override
    public String constructorString(String fieldName) {
        return constructorStringMap.getOrDefault(fieldName, "");
    }

    @Override
    public List<String> beanProperties(String field) {
        return beanPropertyMap.getOrDefault(field, Collections.emptyList());
    }

    @Override
    public List<String> publicProperties(String field) {
        return publicMemberMap.getOrDefault(field, Collections.emptyList());
    }

    @Override
    public Set<String> getImportClasses() {
        return Collections.unmodifiableSet(importClasses);
    }

    @Override
    public SourceField getFieldForName(String name) {
        for (Field f : nodeFields) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SourceCbMethodHandle> List<T> getDispatchMapForGraph() {
        return (List<T>) Collections.unmodifiableList(allEventCallBacks);
    }

    @Override
    public List<String> getOnTriggerDependentCallbackKeys(SourceCbMethodHandle cbHandle) {
        String key = cbHandle.getVariableName() + "::" + cbHandle.getMethodName();
        return onTriggerDependentCallbackKeys.getOrDefault(key, Collections.emptyList());
    }

    @Override
    public Map<MethodDescriptor, ExportFunctionData> getExportedFunctionMap() {
        return Collections.unmodifiableMap(exportedFunctionMap);
    }

    @Override
    public String getGraphMlOutput() {
        return graphMlOutput;
    }

    @Override
    public byte[] getPngOutput() {
        return pngOutput;
    }

    @Override
    public void resolveInstances(Map<String, Object> instanceMap) {
        // resolve Field instances
        for (Field f : nodeFields) {
            Object inst = instanceMap.get(f.getName());
            if (inst != null) {
                f.setInstance(inst);
            }
        }
        for (Field f : topologicallySortedNodeFields) {
            Object inst = instanceMap.get(f.getName());
            if (inst != null) {
                f.setInstance(inst);
            }
        }
        for (Field f : nodeRegistrationListenerFields) {
            Object inst = instanceMap.get(f.getName());
            if (inst != null) {
                f.setInstance(inst);
            }
        }
        // resolve CbMethodHandle instances and Method references
        Map<Class<?>, Map<String, java.lang.reflect.Method>> methodCache = new HashMap<>();
        resolveCbs(instanceMap, methodCache, initialiseMethods);
        resolveCbs(instanceMap, methodCache, startMethods);
        resolveCbs(instanceMap, methodCache, startCompleteMethods);
        resolveCbs(instanceMap, methodCache, stopMethods);
        resolveCbs(instanceMap, methodCache, batchPauseMethods);
        resolveCbs(instanceMap, methodCache, eventEndMethods);
        resolveCbs(instanceMap, methodCache, batchEndMethods);
        resolveCbs(instanceMap, methodCache, tearDownMethods);
        resolveCbs(instanceMap, methodCache, allEventCallBacks);
        resolveCbs(instanceMap, methodCache, allPostEventCallBacks);
        for (List<CbMethodHandle> cbs : parentUpdateListenerMethodMap.values()) {
            resolveCbs(instanceMap, methodCache, cbs);
        }
        for (Map<FilterDescription, List<CbMethodHandle>> filterMap : dispatchMap.values()) {
            for (List<CbMethodHandle> cbs : filterMap.values()) {
                resolveCbs(instanceMap, methodCache, cbs);
            }
        }
        for (Map<FilterDescription, List<CbMethodHandle>> filterMap : postDispatchMap.values()) {
            for (List<CbMethodHandle> cbs : filterMap.values()) {
                resolveCbs(instanceMap, methodCache, cbs);
            }
        }
        for (Map<FilterDescription, List<CbMethodHandle>> filterMap : handlerOnlyDispatchMap.values()) {
            for (List<CbMethodHandle> cbs : filterMap.values()) {
                resolveCbs(instanceMap, methodCache, cbs);
            }
        }
    }

    private void resolveCbs(Map<String, Object> instanceMap,
                            Map<Class<?>, Map<String, java.lang.reflect.Method>> methodCache,
                            List<CbMethodHandle> cbs) {
        for (CbMethodHandle cb : cbs) {
            if (cb.getInstance() != null && cb.getMethod() != null) continue;
            Object inst = instanceMap.get(cb.getVariableName());
            if (inst != null) {
                cb.setInstance(inst);
                java.lang.reflect.Method m = findMethod(inst.getClass(), cb.getMethodString(), methodCache);
                if (m == null && cb.getMethodName() != null) {
                    for (java.lang.reflect.Method candidate : inst.getClass().getMethods()) {
                        if (candidate.getName().equals(cb.getMethodName())
                                && candidate.getParameterCount() == cb.getParameterCount()) {
                            m = candidate;
                            break;
                        }
                    }
                }
                if (m != null) cb.setMethod(m);
            }
        }
    }

    private static java.lang.reflect.Method findMethod(Class<?> clazz, String methodString,
                                                        Map<Class<?>, Map<String, java.lang.reflect.Method>> methodCache) {
        if (methodString == null) return null;
        Map<String, java.lang.reflect.Method> cache = methodCache.computeIfAbsent(clazz, c -> {
            Map<String, java.lang.reflect.Method> m = new HashMap<>();
            for (java.lang.reflect.Method method : c.getMethods()) m.put(method.toGenericString(), method);
            return m;
        });
        return cache.get(methodString);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Setters — used by the toDto() conversion on the server side
    // ═══════════════════════════════════════════════════════════════════

    public void setInitialiseMethods(List<CbMethodHandle> v) { this.initialiseMethods = v; }
    public void setStartMethods(List<CbMethodHandle> v) { this.startMethods = v; }
    public void setStartCompleteMethods(List<CbMethodHandle> v) { this.startCompleteMethods = v; }
    public void setStopMethods(List<CbMethodHandle> v) { this.stopMethods = v; }
    public void setBatchPauseMethods(List<CbMethodHandle> v) { this.batchPauseMethods = v; }
    public void setEventEndMethods(List<CbMethodHandle> v) { this.eventEndMethods = v; }
    public void setBatchEndMethods(List<CbMethodHandle> v) { this.batchEndMethods = v; }
    public void setTearDownMethods(List<CbMethodHandle> v) { this.tearDownMethods = v; }
    public void setDispatchOnlyVersion(boolean v) { this.dispatchOnlyVersion = v; }
    public void setNodeFields(List<Field> v) { this.nodeFields = v; }
    public void setTopologicallySortedNodeFields(List<Field> v) { this.topologicallySortedNodeFields = v; }
    public void setNodeRegistrationListenerFields(List<Field> v) { this.nodeRegistrationListenerFields = v; }
    public void setDispatchMap(Map<String, Map<FilterDescription, List<CbMethodHandle>>> v) { this.dispatchMap = v; }
    public void setPostDispatchMap(Map<String, Map<FilterDescription, List<CbMethodHandle>>> v) { this.postDispatchMap = v; }
    public void setHandlerOnlyDispatchMap(Map<String, Map<FilterDescription, List<CbMethodHandle>>> v) { this.handlerOnlyDispatchMap = v; }
    public void setAllEventCallBacks(List<CbMethodHandle> v) { this.allEventCallBacks = v; }
    public void setAllPostEventCallBacks(List<CbMethodHandle> v) { this.allPostEventCallBacks = v; }
    public void setFilterDescriptionList(List<FilterDescription> v) { this.filterDescriptionList = v; }
    public void setDirtyFieldMap(Map<String, DirtyFlag> v) { this.dirtyFieldMap = v; }
    public void setNodeGuardMap(Map<String, List<DirtyFlag>> v) { this.nodeGuardMap = v; }
    public void setSupportDirtyFiltering(boolean v) { this.supportDirtyFiltering = v; }
    public void setParentUpdateListenerMethodMap(Map<String, List<CbMethodHandle>> v) { this.parentUpdateListenerMethodMap = v; }
    public void setDirectParentMap(Map<String, List<String>> v) { this.directParentMap = v; }
    public void setClass2CanonicalNameMap(Map<String, String> v) { this.class2CanonicalNameMap = v; }
    public void setHierarchySortedClassList(List<String> v) { this.hierarchySortedClassList = v; }
    public void setImportClasses(Set<String> v) { this.importClasses = v; }
    public void setClass2ReplaceMap(Map<String, String> v) { this.class2ReplaceMap = v; }
    public void setConstructorStringMap(Map<String, String> v) { this.constructorStringMap = v; }
    public void setBeanPropertyMap(Map<String, List<String>> v) { this.beanPropertyMap = v; }
    public void setPublicMemberMap(Map<String, List<String>> v) { this.publicMemberMap = v; }
    public void setTypeMap(Map<String, String> v) { this.typeMap = v; }
    public void setOnTriggerDependentCallbackKeys(Map<String, List<String>> v) { this.onTriggerDependentCallbackKeys = v; }
    public void setExportedFunctionMap(Map<MethodDescriptor, ExportFunctionData> v) { this.exportedFunctionMap = v; }
    public void setGraphMlOutput(String v) { this.graphMlOutput = v; }
    public void setPngOutput(byte[] v) { this.pngOutput = v; }
}
