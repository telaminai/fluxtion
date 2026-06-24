/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.model;

import com.telamin.fluxtion.runtime.meta.dto.TopologicallySortedDependencyGraphDto;

import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * ServiceLoader interface for model generation. Implementations perform the analysis of a
 * {@link TopologicallySortedDependencyGraphDto} and return a fully populated {@link EventProcessorModel}.
 *
 * <p>Providers wrap {@code SimpleEventProcessorModel} construction and generation. Implementations
 * may perform this work locally or remotely without requiring live object references on the
 * client side.</p>
 *
 * <p>The active implementation is selected via the system property
 * {@value #MODEL_GENERATOR_ID_PROPERTY}. If the property is absent or no matching id is found,
 * the first registered provider is used.</p>
 */
public interface SimpleEventProcessorModelGenerator {

    String MODEL_GENERATOR_ID_PROPERTY = "fluxtion.modelGeneratorId";

    /**
     * @return a unique identifier for this implementation (e.g. {@code "local"}, {@code "remote"}).
     */
    String id();

    /**
     * Generate a fully populated {@link EventProcessorModel} from the supplied DTO.
     *
     * <p>The DTO carries all config flags and node metadata. The generation path is purely
     * DTO-driven — no live object references or user class access is required.</p>
     *
     * @param dto the serializable descriptor of the graph and all generation config;
     *            built by {@code TopologicallySortedDependencyGraphDtoBuilder}
     * @return a ready-to-use {@link EventProcessorModel}
     * @throws Exception if model generation fails
     */
    EventProcessorModel generate(TopologicallySortedDependencyGraphDto dto) throws Exception;

    /**
     * Load the appropriate {@link SimpleEventProcessorModelGenerator} implementation via
     * {@link ServiceLoader}. The implementation whose {@link #id()} matches the system property
     * {@value #MODEL_GENERATOR_ID_PROPERTY} is preferred; if the property is unset the first
     * available provider is returned.
     *
     * @return the selected {@link SimpleEventProcessorModelGenerator}
     * @throws IllegalStateException if no provider is registered on the classpath
     */
    static SimpleEventProcessorModelGenerator load() {
        Logger log = Logger.getLogger(SimpleEventProcessorModelGenerator.class.getName());
        String desiredId = System.getProperty(MODEL_GENERATOR_ID_PROPERTY, "").trim();
        ServiceLoader<SimpleEventProcessorModelGenerator> loader =
                ServiceLoader.load(SimpleEventProcessorModelGenerator.class);

        if (!desiredId.isEmpty()) {
            for (SimpleEventProcessorModelGenerator gen : loader) {
                if (desiredId.equalsIgnoreCase(gen.id())) {
                    log.fine(() -> "Selected SimpleEventProcessorModelGenerator by id '" + desiredId + "' -> " + gen.getClass().getName());
                    return gen;
                }
            }
            throw new IllegalStateException(
                    "No SimpleEventProcessorModelGenerator found with id '" + desiredId +
                    "'. Set system property '" + MODEL_GENERATOR_ID_PROPERTY +
                    "' to a valid id or ensure the implementation is on the classpath.");
        }

        // No explicit ID requested, try default search order: 1. local, 2. remote-http
        SimpleEventProcessorModelGenerator localGen = null;
        SimpleEventProcessorModelGenerator remoteHttpGen = null;
        SimpleEventProcessorModelGenerator first = null;
        for (SimpleEventProcessorModelGenerator gen : loader) {
            if (first == null) first = gen;
            if ("local".equalsIgnoreCase(gen.id())) {
                localGen = gen;
            } else if ("remote-http".equalsIgnoreCase(gen.id())) {
                remoteHttpGen = gen;
            }
        }

        if (localGen != null) {
            final SimpleEventProcessorModelGenerator finalLocalGen = localGen;
            log.fine(() -> "Selected default SimpleEventProcessorModelGenerator (local) -> " + finalLocalGen.getClass().getName());
            return localGen;
        }
        if (remoteHttpGen != null) {
            final SimpleEventProcessorModelGenerator finalRemoteHttpGen = remoteHttpGen;
            log.fine(() -> "Selected default SimpleEventProcessorModelGenerator (remote-http) -> " + finalRemoteHttpGen.getClass().getName());
            return remoteHttpGen;
        }
        if (first != null) {
            SimpleEventProcessorModelGenerator selected = first;
            log.fine(() -> "Selected default SimpleEventProcessorModelGenerator (first available) -> " +
                    selected.getClass().getName() + " (id='" + selected.id() + "')");
            return selected;
        }
        throw new IllegalStateException(
                "No SimpleEventProcessorModelGenerator providers discovered via ServiceLoader. " +
                "Ensure a model generator implementation is on the classpath.");
    }
}
