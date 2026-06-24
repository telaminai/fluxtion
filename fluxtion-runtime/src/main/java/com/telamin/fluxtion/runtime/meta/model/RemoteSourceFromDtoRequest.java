/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.model;

import com.telamin.fluxtion.runtime.meta.dto.TopologicallySortedDependencyGraphDto;

import java.io.Serializable;
import java.util.Objects;

/**
 * Combined request DTO for single-call remote source generation. Sends the
 * graph DTO and source-gen config in one request — the server generates the
 * model internally and returns only the source string. The model never leaves
 * the server, reducing network overhead and IP leakage.
 */
public class RemoteSourceFromDtoRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private TopologicallySortedDependencyGraphDto dto;
    private SourceGenConfig sourceGenConfig;

    public RemoteSourceFromDtoRequest() {}

    public RemoteSourceFromDtoRequest(TopologicallySortedDependencyGraphDto dto, SourceGenConfig sourceGenConfig) {
        this.dto = dto;
        this.sourceGenConfig = sourceGenConfig;
    }

    public TopologicallySortedDependencyGraphDto getDto() { return dto; }
    public void setDto(TopologicallySortedDependencyGraphDto dto) { this.dto = dto; }

    public SourceGenConfig getSourceGenConfig() { return sourceGenConfig; }
    public void setSourceGenConfig(SourceGenConfig sourceGenConfig) { this.sourceGenConfig = sourceGenConfig; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteSourceFromDtoRequest that = (RemoteSourceFromDtoRequest) o;
        return Objects.equals(dto, that.dto) && Objects.equals(sourceGenConfig, that.sourceGenConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dto, sourceGenConfig);
    }

    @Override
    public String toString() {
        return "RemoteSourceFromDtoRequest{dto=" + dto + ", sourceGenConfig=" + sourceGenConfig + '}';
    }
}
