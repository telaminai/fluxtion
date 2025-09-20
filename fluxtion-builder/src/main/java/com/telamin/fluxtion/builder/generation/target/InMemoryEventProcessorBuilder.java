/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.generation.target;

import com.telamin.fluxtion.builder.generation.config.EventProcessorConfig;
import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import com.telamin.fluxtion.builder.generation.model.SimpleEventProcessorModel;
import com.telamin.fluxtion.builder.generation.model.TopologicallySortedDependencyGraph;
import com.telamin.fluxtion.builder.node.NodeFactoryLocator;
import com.telamin.fluxtion.builder.node.NodeFactoryRegistration;
import com.telamin.fluxtion.runtime.partition.LambdaReflection;
import lombok.Getter;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.telamin.fluxtion.builder.generation.context.GenerationContext.*;

@Getter
public class InMemoryEventProcessorBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryEventProcessorBuilder.class);
    private SimpleEventProcessorModel simpleEventProcessorModel;

    @SneakyThrows
    public static InMemoryEventProcessor interpreted(LambdaReflection.SerializableConsumer<EventProcessorConfig> cfgBuilder, boolean generateDescription) {
        EventProcessorConfig cfg = new EventProcessorConfig();
        String pkg = (cfgBuilder.getContainingClass().getCanonicalName() + "." + cfgBuilder.method().getName()).toLowerCase();
        GenerationContext.setupStaticContext(pkg, "Processor", new File(JAVA_GEN_DIR), new File(RESOURCE_DIR));
        cfgBuilder.accept(cfg);
        return new InMemoryEventProcessorBuilder().inMemoryProcessor(cfg, generateDescription);
    }

    @SneakyThrows
    public static InMemoryEventProcessor interpretedTest(LambdaReflection.SerializableConsumer<EventProcessorConfig> cfgBuilder) {
        EventProcessorConfig cfg = new EventProcessorConfig();
        cfgBuilder.accept(cfg);
        InMemoryEventProcessorBuilder eventProcessorGenerator = new InMemoryEventProcessorBuilder();
        String pkg = (cfgBuilder.getContainingClass().getCanonicalName() + "." + cfgBuilder.method().getName()).toLowerCase();
        GenerationContext.setupStaticContext(pkg, "Processor", new File(JAVA_TESTGEN_DIR), new File(RESOURCE_GENERATED_TEST_DIR));
        return eventProcessorGenerator.inMemoryProcessor(cfg, false);
    }

    public InMemoryEventProcessor inMemoryProcessor(EventProcessorConfig config, boolean generateDescription) throws Exception {
        config.buildConfig();
        LOG.debug("locateFactories");
        if (config.getNodeFactoryRegistration() == null) {
            config.setNodeFactoryRegistration(new NodeFactoryRegistration(NodeFactoryLocator.nodeFactorySet()));
        } else {
            config.getNodeFactoryRegistration().factoryClassSet.addAll(NodeFactoryLocator.nodeFactorySet());
        }
        if (GenerationContext.SINGLETON == null) {
            GenerationContext.setupStaticContext("", "", null, null);
        }
        if (GenerationContext.SINGLETON == null) {
            throw new RuntimeException("could not initialise Generations.SINGLETON context");
        }
        TopologicallySortedDependencyGraph graph = new TopologicallySortedDependencyGraph(
                config.getNodeList(),
                config.getPublicNodes(),
                config.getNodeFactoryRegistration(),
                GenerationContext.SINGLETON,
                config.getAuditorMap(),
                config
        );
        simpleEventProcessorModel = new SimpleEventProcessorModel(graph, config.getFilterMap(), GenerationContext.SINGLETON.getProxyClassMap());
        simpleEventProcessorModel.generateMetaModelInMemory(config.isSupportDirtyFiltering());
        GenerationContext.resetStaticContext();
        return new InMemoryEventProcessor(simpleEventProcessorModel, config);
    }

}
