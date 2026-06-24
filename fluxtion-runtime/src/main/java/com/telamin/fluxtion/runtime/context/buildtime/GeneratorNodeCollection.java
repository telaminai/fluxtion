/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.context.buildtime;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service providing buildtime access to constructing a SEP, use {@link #service() } to gain runtime access to the
 * context.
 *
 * @author 2024 gregory higgins.
 */
public interface GeneratorNodeCollection {

    AtomicLong currentIdAdder = new AtomicLong();

    default int nextSequenceNumber(int currentGenerationId) {
        currentIdAdder.incrementAndGet();
        long currentId = currentIdAdder.longValue();
        if (currentGenerationId < currentId) {
            currentGenerationId++;
            currentId++;
        } else if (currentGenerationId >= currentId) {
            currentGenerationId = 1;
            currentId++;
        }
        return currentGenerationId;
    }

    static void resetGenerationContext() {
        currentIdAdder.set(0);
        currentIdAdder.incrementAndGet();
    }

    <T> T add(T node);

    <T> T[] add(T... nodes);

    <T> T add(T node, String privateId);

    <T> T addPublic(T node, String publicId);

    <T> T addOrReuse(T node);

    <T> T[] addOrReuse(T... nodes);

    <T> T addOrReuse(T node, String privateId);

    <T> T addPublicOrReuse(T node, String publicId);

    <T> T getNodeById(String id);

    default boolean buildTime() {
        return false;
    }

    GeneratorNodeCollection NULL_CONTEXT = new GeneratorNodeCollection() {

        @Override
        public <T> T add(T node) {
            return node;
        }

        @Override
        public <T> T[] add(T... nodes) {
            return nodes;
        }

        @Override
        public <T> T[] addOrReuse(T... nodes) {
            return nodes;
        }

        @Override
        public <T> T addPublic(T node, String publicId) {
            return node;
        }

        @Override
        public <T> T add(T node, String privateId) {
            return node;
        }

        @Override
        public <T> T addOrReuse(T node) {
            return node;
        }

        @Override
        public <T> T addOrReuse(T node, String privateId) {
            return node;
        }

        @Override
        public <T> T addPublicOrReuse(T node, String publicId) {
            return node;
        }

        @Override
        public <T> T getNodeById(String id) {
            return null;
        }
    };

    static GeneratorNodeCollection service() {
        ServiceLoader<GeneratorNodeCollection> load = ServiceLoader.load(GeneratorNodeCollection.class, GeneratorNodeCollection.class.getClassLoader());
        GeneratorNodeCollection service = NULL_CONTEXT;
        if (load.iterator().hasNext()) {
            service = load.iterator().next();
            return service.buildTime() ? service : NULL_CONTEXT;
        } else {
            load = ServiceLoader.load(GeneratorNodeCollection.class);
            if (load.iterator().hasNext()) {
                service = load.iterator().next();
                return service.buildTime() ? service : NULL_CONTEXT;
            } else {
                return NULL_CONTEXT;
            }
        }
    }

    static int nextId(int currentGenerationId) {
        return service().nextSequenceNumber(currentGenerationId);
    }
}
