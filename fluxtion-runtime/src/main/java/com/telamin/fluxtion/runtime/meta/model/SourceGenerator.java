/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.meta.model;

import java.io.Writer;

/**
 * Interface for source code generators that produce event processor source from a model.
 * Implementations are discovered via {@link java.util.ServiceLoader}.
 */
public interface SourceGenerator {
    String id();

    void generateDataFlowSource(
            EventProcessorModel model,
            SourceGenConfig sourceGenConfig,
            Writer templateWriter) throws Exception;
}
