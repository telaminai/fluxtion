/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.generation.target;

import com.fluxtion.dataflow.builder.generation.config.EventProcessorConfig;
import com.fluxtion.dataflow.builder.generation.context.GenerationContext;
import com.fluxtion.dataflow.builder.generation.model.SimpleEventProcessorModel;
import com.fluxtion.dataflow.builder.generation.model.TopologicallySortedDependencyGraph;
import com.fluxtion.dataflow.builder.node.NodeFactoryLocator;
import com.fluxtion.dataflow.builder.node.NodeFactoryRegistration;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection;
import lombok.Getter;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.fluxtion.dataflow.builder.generation.context.GenerationContext.*;

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
