/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.compile.generation.remote.common;

import com.telamin.fluxtion.builder.compile.config.FluxtionCompilerConfig;
import com.telamin.fluxtion.builder.generation.config.EventProcessorConfig;
import com.telamin.fluxtion.builder.generation.model.EventProcessorModel;
import lombok.ToString;

import java.io.Serializable;
import java.util.Set;

/**
 * A simple request DTO for remote generation. Contains a serializable subset of
 * the configuration and context needed by the server to execute the generation.
 */
@ToString
public class RemoteGenerationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private EventProcessorModel model;

    // minimal config needed by VelocityGenerator
    private boolean inlineEventHandling;
    private boolean supportBufferAndTrigger;
    private EventProcessorConfig.DISPATCH_STRATEGY dispatchStrategy;
    private String templateFile;
    private Set<Class<?>> interfacesToImplement;

    // compiler config + naming context
    private FluxtionCompilerConfig compilerConfig;
    private String packageName;
    private String className;

    public EventProcessorModel getModel() { return model; }
    public void setModel(EventProcessorModel model) { this.model = model; }

    public boolean isInlineEventHandling() { return inlineEventHandling; }
    public void setInlineEventHandling(boolean inlineEventHandling) { this.inlineEventHandling = inlineEventHandling; }

    public boolean isSupportBufferAndTrigger() { return supportBufferAndTrigger; }
    public void setSupportBufferAndTrigger(boolean supportBufferAndTrigger) { this.supportBufferAndTrigger = supportBufferAndTrigger; }

    public EventProcessorConfig.DISPATCH_STRATEGY getDispatchStrategy() { return dispatchStrategy; }
    public void setDispatchStrategy(EventProcessorConfig.DISPATCH_STRATEGY dispatchStrategy) { this.dispatchStrategy = dispatchStrategy; }

    public String getTemplateFile() { return templateFile; }
    public void setTemplateFile(String templateFile) { this.templateFile = templateFile; }

    public Set<Class<?>> getInterfacesToImplement() { return interfacesToImplement; }
    public void setInterfacesToImplement(Set<Class<?>> interfacesToImplement) { this.interfacesToImplement = interfacesToImplement; }

    public FluxtionCompilerConfig getCompilerConfig() { return compilerConfig; }
    public void setCompilerConfig(FluxtionCompilerConfig compilerConfig) { this.compilerConfig = compilerConfig; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
}
