/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.compile.config;

import com.telamin.fluxtion.builder.generation.config.EventProcessorConfig;
import com.telamin.fluxtion.builder.generation.config.RootNodeConfig;
import com.telamin.fluxtion.runtime.audit.EventLogControlEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Map;

/**
 * Combines {@link RootNodeConfig} and {@link FluxtionCompilerConfig} into a single instance
 * so a complete configuration for a generation run can be recorded.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataDrivenGenerationConfig {

    private String name;
    private String rootClass;
    private Map<String, Object> configMap;
    private List<Object> nodes;
    private boolean enableAudit;
    private boolean printEventToString = false;
    private EventLogControlEvent.LogLevel traceLogeLevel = EventLogControlEvent.LogLevel.NONE;
    private FluxtionCompilerConfig compilerConfig;// = new FluxtionCompilerConfig();

    @SneakyThrows
    public RootNodeConfig getRootNodeConfig() {
        Class<?> rootClass1 = rootClass == null ? null : Class.forName(rootClass, true, compilerConfig.getClassLoader());
        return new RootNodeConfig(name, rootClass1, configMap, nodes);
    }

    public EventProcessorConfig getEventProcessorConfig() {
        EventProcessorConfig eventProcessorConfig = new EventProcessorConfig();
        eventProcessorConfig.setRootNodeConfig(getRootNodeConfig());
        if (enableAudit) {
            eventProcessorConfig.addEventAudit();
        }
        if (traceLogeLevel != EventLogControlEvent.LogLevel.NONE) {
            eventProcessorConfig.addEventAudit(traceLogeLevel, printEventToString);
        }
        return eventProcessorConfig;
    }
}
