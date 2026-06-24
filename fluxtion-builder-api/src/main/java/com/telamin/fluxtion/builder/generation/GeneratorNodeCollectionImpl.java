/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.builder.generation;

import com.google.auto.service.AutoService;
import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import com.telamin.fluxtion.builder.generation.context.GenerationContextHolder;
import com.telamin.fluxtion.runtime.context.buildtime.GeneratorNodeCollection;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * @author 2024 gregory higgins.
 */
@AutoService(GeneratorNodeCollection.class)
public class GeneratorNodeCollectionImpl implements GeneratorNodeCollection {

    private static final class SequenceState {
        private long currentId = 1;
    }

    public static void resetGenerationContext() {
        GenerationContext context = GenerationContextHolder.currentOrNull();
        if (context != null) {
            context.removeCache(SequenceState.class);
        }
        GeneratorNodeCollection.resetGenerationContext();
    }

    @Override
    public int nextSequenceNumber(int currentGenerationId) {
        GenerationContext context = GenerationContextHolder.ensureInlineContext();
        SequenceState sequenceState = context.<Class<?>, SequenceState>getCache(
                SequenceState.class).computeIfAbsent(
                SequenceState.class,
                key -> new SequenceState());
        if (currentGenerationId < sequenceState.currentId) {
            currentGenerationId++;
            sequenceState.currentId++;
        } else if (currentGenerationId >= sequenceState.currentId) {
            currentGenerationId = 1;
            sequenceState.currentId++;
        }
        return currentGenerationId;
    }

    @Override
    public <T> T add(T node) {
        GenerationContextHolder.ensureInlineContext().getNodeList().add(node);
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
        GenerationContextHolder.ensureInlineContext().getPublicNodes().put(node, publicId);
        return node;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T add(T node, String privateId) {
        GenerationContext context = GenerationContextHolder.ensureInlineContext();
        context.getNodeList().add(node);
        context.nameNode(node, privateId);
        return node;
    }

    @Override
    public <T> T addOrReuse(T node) {
        return GenerationContextHolder.ensureInlineContext().addOrUseExistingNode(node);
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
        GenerationContext context = GenerationContextHolder.ensureInlineContext();
        node = context.addOrUseExistingNode(node);
        context.nameNode(node, privateId);
        return node;
    }

    @Override
    public <T> T addPublicOrReuse(T node, String publicId) {
        GenerationContext context = GenerationContextHolder.ensureInlineContext();
        node = context.addOrUseExistingNode(node);
        context.getPublicNodes().put(node, publicId);
        return node;
    }

    @Override
    public <T> T getNodeById(String id) {
        Optional<Object> optional = GenerationContextHolder.ensureInlineContext()
                .getPublicNodes().entrySet().stream()
                .filter(e -> e.getValue().equals(id))
                .findFirst()
                .map(Entry::getKey);
        return (T) optional.orElse(null);
    }

    @Override
    public boolean buildTime() {
        return GenerationContextHolder.currentOrNull() != null;
    }
}
