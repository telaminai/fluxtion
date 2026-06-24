/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * A simple request DTO for remote generation. Contains the model and all
 * configuration needed by the server to execute the generation, packaged
 * in a single {@link SourceGenConfig}.
 */
public class RemoteGenerationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private EventProcessorModel model;
    private SourceGenConfig sourceGenConfig;

    public RemoteGenerationRequest() {}

    public RemoteGenerationRequest(EventProcessorModel model, SourceGenConfig sourceGenConfig) {
        this.model = model;
        this.sourceGenConfig = sourceGenConfig;
    }

    public EventProcessorModel getModel() { return model; }
    public void setModel(EventProcessorModel model) { this.model = model; }

    public SourceGenConfig getSourceGenConfig() { return sourceGenConfig; }
    public void setSourceGenConfig(SourceGenConfig sourceGenConfig) { this.sourceGenConfig = sourceGenConfig; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteGenerationRequest that = (RemoteGenerationRequest) o;
        return Objects.equals(model, that.model) && Objects.equals(sourceGenConfig, that.sourceGenConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, sourceGenConfig);
    }

    @Override
    public String toString() {
        return "RemoteGenerationRequest{" +
                "model=" + model +
                ", sourceGenConfig=" + sourceGenConfig +
                '}';
    }
}
