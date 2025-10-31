/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.extern.spring;

import com.telamin.fluxtion.Fluxtion;
import com.telamin.fluxtion.builder.compile.config.FluxtionCompilerConfig;
import com.telamin.fluxtion.builder.generation.config.EventProcessorConfig;
import com.telamin.fluxtion.builder.generation.context.RuntimeConstants;
import com.telamin.fluxtion.runtime.CloneableDataFlow;
import com.telamin.fluxtion.runtime.audit.Auditor;
import com.telamin.fluxtion.runtime.partition.LambdaReflection;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.util.ClassUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Provides utility functions to build Fluxtion {@link CloneableDataFlow} using a spring {@link ApplicationContext} to define
 * the object instances managed by Fluxtion.
 */
public class FluxtionSpring {

    private final static Logger LOGGER = LoggerFactory.getLogger(FluxtionSpring.class);
    private final ApplicationContext context;
    private Consumer<EventProcessorConfig> configCustomizer = c -> {
    };

    public FluxtionSpring(String springFile) {
        this(new FileSystemXmlApplicationContext(springFile));
        LOGGER.debug("loading spring springFile:{}", springFile);
    }

    public FluxtionSpring(String springFile, Consumer<EventProcessorConfig> configCustomizer) {
        this(new FileSystemXmlApplicationContext(springFile), configCustomizer);
        LOGGER.debug("loading spring springFile:{}", springFile);
    }

    public FluxtionSpring(ApplicationContext context, Consumer<EventProcessorConfig> configCustomizer) {
        this.context = context;
        this.configCustomizer = configCustomizer;
    }

    public FluxtionSpring(ApplicationContext context) {
        this.context = context;
    }

    public static CloneableDataFlow<?> compileAot(File springFile, String className, String packageName) {
        return new FluxtionSpring(springFile.toURI().toString())._compileAot(
                c -> {
                    c.setClassName(className);
                    c.setPackageName(packageName);
                });
    }

    @SneakyThrows
    public static CloneableDataFlow<?> compileAot(ClassLoader classLoader, File springFile, String className, String packageName) {
        ClassUtils.overrideThreadContextClassLoader(classLoader);
        return new FluxtionSpring(springFile.toURI().toString())._compileAot(c -> {
            c.setClassName(className);
            c.setPackageName(packageName);
            c.setCompileSource(false);
            String overrideOutputDirectory = System.getProperty(RuntimeConstants.OUTPUT_DIRECTORY);
            if (overrideOutputDirectory != null && !overrideOutputDirectory.isEmpty()) {
                c.setOutputDirectory(overrideOutputDirectory);
            }
            String overrideResourceDirectory = System.getProperty(RuntimeConstants.RESOURCES_DIRECTORY);
            if (overrideResourceDirectory != null && !overrideResourceDirectory.isEmpty()) {
                c.setResourcesOutputDirectory(overrideResourceDirectory);
            }
        });
    }

    public static CloneableDataFlow<?> compileAot(
            Path springFile,
            LambdaReflection.SerializableConsumer<FluxtionCompilerConfig> compilerConfig) {
        FluxtionSpring fluxtionSpring = new FluxtionSpring(springFile.toAbsolutePath().toUri().toString());
        return fluxtionSpring._compileAot(compilerConfig);
    }

    public static CloneableDataFlow<?> compileAot(
            Path springFile,
            Consumer<EventProcessorConfig> configCustomizer,
            LambdaReflection.SerializableConsumer<FluxtionCompilerConfig> compilerConfig) {
        return new FluxtionSpring(springFile.toAbsolutePath().toUri().toString(), configCustomizer)._compileAot(compilerConfig);
    }

    public static CloneableDataFlow<?> compileAot(
            ApplicationContext context,
            LambdaReflection.SerializableConsumer<FluxtionCompilerConfig> compilerConfig) {
        return new FluxtionSpring(context)._compileAot(compilerConfig);
    }

    public static CloneableDataFlow<?> compileAot(ApplicationContext context, String className, String packageName) {
        return new FluxtionSpring(context)._compileAot(c -> {
            c.setClassName(className);
            c.setPackageName(packageName);
        });
    }

    public static CloneableDataFlow<?> compileAot(
            ApplicationContext context,
            Consumer<EventProcessorConfig> configCustomizer,
            LambdaReflection.SerializableConsumer<FluxtionCompilerConfig> compilerConfig) {
        return new FluxtionSpring(context, configCustomizer)._compileAot(compilerConfig);
    }

    public static CloneableDataFlow<?> compile(Path springFile) {
        FluxtionSpring fluxtionSpring = new FluxtionSpring(springFile.toAbsolutePath().toUri().toString());
        return fluxtionSpring._compile();
    }

    public static CloneableDataFlow<?> compile(Path springFile, Consumer<EventProcessorConfig> configCustomizer) {
        return new FluxtionSpring(springFile.toAbsolutePath().toUri().toString(), configCustomizer)._compile();
    }

    public static CloneableDataFlow<?> compile(ApplicationContext context) {
        return new FluxtionSpring(context)._compile();
    }

    public static CloneableDataFlow<?> compile(ApplicationContext context, Consumer<EventProcessorConfig> configCustomizer) {
        return new FluxtionSpring(context, configCustomizer)._compile();
    }

    private CloneableDataFlow<?> _compileAot(LambdaReflection.SerializableConsumer<FluxtionCompilerConfig> compilerConfig) {
        return Fluxtion.compile(this::addNodes, compilerConfig);
    }

    private CloneableDataFlow<?> _compile() {
        return Fluxtion.compile(this::addNodes);
    }

    protected void addNodes(EventProcessorConfig config) {
        LOGGER.debug("loading spring context:{}", context);
        List<Auditor> auditorMap = new ArrayList<>();
        for (String beanDefinitionName : context.getBeanDefinitionNames()) {
            Object bean = context.getBean(beanDefinitionName);
            if (bean instanceof FluxtionSpringConfig) {
                FluxtionSpringConfig springConfig = (FluxtionSpringConfig) bean;
                auditorMap.addAll(springConfig.getAuditors());
                config.addEventAudit(springConfig.getLogLevel());
            }
        }

        for (String beanDefinitionName : context.getBeanDefinitionNames()) {
            Object bean = context.getBean(beanDefinitionName);
            if (!(bean instanceof FluxtionSpringConfig)) {
                if (bean instanceof Auditor && auditorMap.contains(bean)) {
                    LOGGER.debug("adding auditor:{} to fluxtion", beanDefinitionName);
                    config.addAuditor((Auditor) bean, beanDefinitionName);
                } else {
                    LOGGER.debug("adding bean:{} to fluxtion", beanDefinitionName);
                    config.addNode(bean, beanDefinitionName);
                }
            }
        }
        configCustomizer.accept(config);
    }
}
