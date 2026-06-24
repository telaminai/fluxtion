package com.telamin.fluxtion.builder.generation.context;

import java.util.List;
import java.util.Map;

/**
 * Build-scoped state shared by authoring code and compiler implementations.
 * Implementations collect authored nodes; graph analysis remains outside this
 * contract.
 */
public interface GenerationContext {

    int nextId(String className);

    List<Object> getNodeList();

    Map<Object, String> getPublicNodes();

    ClassLoader getClassLoader();

    <T> T addOrUseExistingNode(T node);

    <K, V> Map<K, V> getCache(Object key);

    <T> T nameNode(T node, String name);

    <K, V> Map<K, V> removeCache(Object key);
}
