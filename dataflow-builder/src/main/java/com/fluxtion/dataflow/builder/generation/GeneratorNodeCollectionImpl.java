/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */
package com.fluxtion.dataflow.builder.generation;

import com.fluxtion.dataflow.builder.generation.context.GenerationContext;
import com.fluxtion.dataflow.runtime.context.buildtime.GeneratorNodeCollection;
import com.google.auto.service.AutoService;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * @author 2024 gregory higgins.
 */
@AutoService(GeneratorNodeCollection.class)
public class GeneratorNodeCollectionImpl implements GeneratorNodeCollection {

    private static long currentId = 1;

    public static void resetGenerationContext() {
        currentId = 1;
        GeneratorNodeCollection.resetGenerationContext();
    }

    @Override
    public int nextSequenceNumber(int currentGenerationId) {
        if (currentGenerationId < currentId) {
            currentGenerationId++;
            currentId++;
        } else if (currentGenerationId >= currentId) {
            currentGenerationId = 1;
            currentId++;
        }
        return currentGenerationId;
    }

    @Override
    public <T> T add(T node) {
        GenerationContext.SINGLETON.getNodeList().add(node);
        return node;
    }

    @SafeVarargs
    @Override
    public final <T> T[] add(T... nodes) {
        ArrayList<T> out = new ArrayList<>();
        for (T node : nodes) {
            out.add(add(node));
        }
        return out.toArray(nodes);
    }

    @Override
    public <T> T addPublic(T node, String publicId) {
        GenerationContext.SINGLETON.getPublicNodes().put(node, publicId);
        return node;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T add(T node, String privateId) {
        GenerationContext.SINGLETON.getNodeList().add(node);
        GenerationContext.SINGLETON.nameNode(node, privateId);
        return node;
    }

    @Override
    public <T> T addOrReuse(T node) {
        return GenerationContext.SINGLETON.addOrUseExistingNode(node);
    }

    @SafeVarargs
    @Override
    public final <T> T[] addOrReuse(T... nodes) {
        ArrayList<T> out = new ArrayList<>();
        for (T node : nodes) {
            out.add(addOrReuse(node));
        }
        return out.toArray(nodes);
    }

    @Override
    public <T> T addOrReuse(T node, String privateId) {
        node = GenerationContext.SINGLETON.addOrUseExistingNode(node);
        GenerationContext.SINGLETON.nameNode(node, privateId);
        return node;
    }

    @Override
    public <T> T addPublicOrReuse(T node, String publicId) {
        node = GenerationContext.SINGLETON.addOrUseExistingNode(node);
        GenerationContext.SINGLETON.getPublicNodes().put(node, publicId);
        return node;
    }

    @Override
    public <T> T getNodeById(String id) {
        Optional<Object> optional = GenerationContext.SINGLETON.getPublicNodes().entrySet().stream()
                .filter(e -> e.getValue().equals(id))
                .findFirst()
                .map(Entry::getKey);
        return (T) optional.orElse(null);
    }

    @Override
    public boolean buildTime() {
        return GenerationContext.SINGLETON != null;
    }
}