/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.generation;

import com.telamin.fluxtion.builder.compile.config.FluxtionCompilerConfig;
import com.telamin.fluxtion.builder.compile.config.OutputRegistry;
import com.telamin.fluxtion.builder.compile.generation.EventProcessorCompilation;
import com.telamin.fluxtion.builder.compile.generation.NodeDispatchTable;
import com.telamin.fluxtion.builder.generation.config.EventProcessorConfig;
import com.telamin.fluxtion.builder.generation.config.RootNodeConfig;
import com.telamin.fluxtion.builder.generation.target.InMemoryEventProcessor;
import com.telamin.fluxtion.builder.generation.target.InMemoryEventProcessorBuilder;
import com.telamin.fluxtion.runtime.CloneableDataFlow;
import com.telamin.fluxtion.runtime.partition.LambdaReflection;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Generates and compiles a SEP for use by a caller in the same process. The compilation is invoked programmatically
 * removing the need to execute the Fluxtion event stream compiler as an external process.<br><br>
 * <p>
 * To generate a SEP the caller invokes one of the static compileSep methods. An instance of {@link EventProcessorConfig} is passed
 * to the consumer to control the graph construction, such as adding nodes and defining scopes or identifiers. Simple
 * example adding a single node:<br><br>
 * <p>
 * {@code  sepTestInstance((c) -> c.addNode(new MyHandler(), "handler"), "com.fluxtion.examples.inprocess", "GenNode_1");}
 * <br><br>
 * <p>
 *
 * @author 2024 gregory higgins.
 */
@Slf4j
public class EventProcessorFactory {

    public static CloneableDataFlow compile(LambdaReflection.SerializableConsumer<EventProcessorConfig> builder) throws Exception {
        String name = "Processor";
        String pkg = (builder.getContainingClass().getCanonicalName() + "." + builder.method().getName()).toLowerCase();
        return compile(name, pkg, builder);
    }

    public static CloneableDataFlow<?> compileDispatcher(LambdaReflection.SerializableConsumer<EventProcessorConfig> builder, Writer sourceWriter) throws Exception {
        String className = "Processor";
        String packageName = (builder.getContainingClass().getCanonicalName() + "." + builder.method().getName()).toLowerCase();

        FluxtionCompilerConfig compilerCfg = new FluxtionCompilerConfig();
        compilerCfg.setDispatchOnlyVersion(true);
        compilerCfg.setInterpreted(false);
        if (sourceWriter != null) {
            compilerCfg.setSourceWriter(sourceWriter);
        }
        compilerCfg.setPackageName(packageName);
        compilerCfg.setClassName(className);
        compilerCfg.setWriteSourceToFile(false);
        compilerCfg.setFormatSource(true);
        compilerCfg.setGenerateDescription(false);

        EventProcessorCompilation compiler = new EventProcessorCompilation();
        Class<CloneableDataFlow<?>> sepClass = compiler.compile(compilerCfg, new InProcessEventProcessorConfig(builder));
        CloneableDataFlow sep = sepClass.getDeclaredConstructor().newInstance();

        Map<String, Object> instanceMap = new HashMap<>();
        compiler.getSimpleEventProcessorModel().getNodeFields().forEach(f -> instanceMap.put(f.getName(), f.getInstance()));
        ((NodeDispatchTable) sep).assignMembers(instanceMap);
        return sep;
    }

    public static CloneableDataFlow compile(RootNodeConfig rootNode) throws Exception {
        LambdaReflection.SerializableConsumer<EventProcessorConfig> builder = (EventProcessorConfig cfg) -> cfg.setRootNodeConfig(rootNode);
        String name = "Processor";
        String pkg = (rootNode.getClass().getCanonicalName() + "." + rootNode.getName()).toLowerCase();
        return compile(pkg, name, builder);
    }

    @SneakyThrows
    public static CloneableDataFlow compile(RootNodeConfig rootNode, LambdaReflection.SerializableConsumer<FluxtionCompilerConfig> cfgBuilder) {
        LambdaReflection.SerializableConsumer<EventProcessorConfig> builder = (EventProcessorConfig cfg) -> cfg.setRootNodeConfig(rootNode);
        return compile(builder, cfgBuilder);
    }

    public static CloneableDataFlow compileTestInstance(Consumer<EventProcessorConfig> cfgBuilder,
                                                        String pckg,
                                                        String sepName,
                                                        boolean writeSourceFile,
                                                        boolean generateMetaInformation) throws Exception {
        return compile(
                cfgBuilder,
                pckg,
                sepName,
                OutputRegistry.JAVA_TESTGEN_DIR,
                OutputRegistry.RESOURCE_GENERATED_TEST_DIR,
                false,
                false,
                writeSourceFile,
                generateMetaInformation);
    }

    public static CloneableDataFlow compileTestInstance(Consumer<EventProcessorConfig> cfgBuilder,
                                                        String pckg,
                                                        String sepName,
                                                        boolean dispatchOnly,
                                                        boolean writeSourceFile,
                                                        boolean generateMetaInformation) throws Exception {
        return compile(
                cfgBuilder,
                pckg,
                sepName,
                OutputRegistry.JAVA_TESTGEN_DIR,
                OutputRegistry.RESOURCE_GENERATED_TEST_DIR,
                dispatchOnly,
                false,
                writeSourceFile,
                generateMetaInformation);
    }

    /**
     * Build a static event processor using the supplied consumer to populate the SEPConfig. Will always build a new
     * processor, supplying a newly created instance of the class to the caller.
     *
     * @param name    The name of the generated static event processor
     * @param pkg     The package name of the generated static event processor
     * @param builder The Consumer that populates the SEPConfig
     * @return An instance of the newly generated static event processor
     * @throws Exception
     */
    public static CloneableDataFlow compile(String name, String pkg, Consumer<EventProcessorConfig> builder) throws Exception {
        return compile(
                builder,
                pkg,
                name,
                OutputRegistry.JAVA_SRC_DIR,
                OutputRegistry.RESOURCE_DIR,
                false,
                false,
                false,
                false);
    }

    /**
     * Compiles and instantiates a SEP described with the provided {@link EventProcessorConfig}, optionally initialising the SEP
     * instance.
     *
     * @param cfgBuilder  - A client consumer to buld sep using the provided
     * @param packageName - output package of the generated class
     * @param className   - output class name of the generated SEP
     * @param srcGenDir   - output directory for generated SEP source files
     * @param resGenDir   - output directory for generated resources
     * @param initialise  - if true call init method on SEP instance
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws Exception
     */
    public static CloneableDataFlow compile(Consumer<EventProcessorConfig> cfgBuilder,
                                            String packageName,
                                            String className,
                                            String srcGenDir,
                                            String resGenDir,
                                            boolean dispatchOnly,
                                            boolean initialise,
                                            boolean writeSourceFile,
                                            boolean generateMetaInformation) throws InstantiationException, IllegalAccessException, Exception {
        EventProcessorCompilation compiler = new EventProcessorCompilation();
        FluxtionCompilerConfig compilerCfg = new FluxtionCompilerConfig();
        compilerCfg.setDispatchOnlyVersion(dispatchOnly);
        compilerCfg.setOutputDirectory(new File(srcGenDir).getCanonicalPath());
        compilerCfg.setResourcesOutputDirectory(new File(resGenDir).getCanonicalPath());
        compilerCfg.setPackageName(packageName);
        compilerCfg.setClassName(className);
        compilerCfg.setWriteSourceToFile(writeSourceFile);
        compilerCfg.setFormatSource(writeSourceFile);
        compilerCfg.setGenerateDescription(generateMetaInformation);

        Class<CloneableDataFlow> sepClass = compiler.compile(compilerCfg, new InProcessEventProcessorConfig(cfgBuilder));
        CloneableDataFlow sep = sepClass.getDeclaredConstructor().newInstance();
        if (dispatchOnly) {
            Map<String, Object> instanceMap = new HashMap<>();
            compiler.getSimpleEventProcessorModel().getNodeFields().forEach(f -> instanceMap.put(f.getName(), f.getInstance()));
            ((NodeDispatchTable) sep).assignMembers(instanceMap);
        }

        if (initialise) {
            sep.init();
        }
        return sep;
    }

    public static CloneableDataFlow compile(
            LambdaReflection.SerializableConsumer<EventProcessorConfig> sepConfig,
            LambdaReflection.SerializableConsumer<FluxtionCompilerConfig> cfgBuilder)
            throws Exception {
        String className = "Processor";
        String packageName = (cfgBuilder.getContainingClass().getCanonicalName() + "." + cfgBuilder.method().getName()).toLowerCase();
        FluxtionCompilerConfig fluxtionCompilerConfig = new FluxtionCompilerConfig();
        fluxtionCompilerConfig.setOutputDirectory(new File(OutputRegistry.JAVA_SRC_DIR).getCanonicalPath());
        fluxtionCompilerConfig.setResourcesOutputDirectory(new File(OutputRegistry.RESOURCE_DIR).getCanonicalPath());
        fluxtionCompilerConfig.setPackageName(packageName);
        fluxtionCompilerConfig.setClassName(className);
        fluxtionCompilerConfig.setWriteSourceToFile(true);
        fluxtionCompilerConfig.setFormatSource(true);
        fluxtionCompilerConfig.setGenerateDescription(true);

        cfgBuilder.accept(fluxtionCompilerConfig);
        EventProcessorCompilation compiler = new EventProcessorCompilation();
        Class<CloneableDataFlow<?>> sepClass = compiler.compile(fluxtionCompilerConfig, new InProcessEventProcessorConfig(sepConfig));
        return sepClass == null ? null : sepClass.getDeclaredConstructor().newInstance();
    }

    public static CloneableDataFlow compile(
            EventProcessorConfig eventProcessorConfig,
            FluxtionCompilerConfig fluxtionCompilerConfig)
            throws Exception {
        EventProcessorCompilation compiler = new EventProcessorCompilation();
        Class<CloneableDataFlow<?>> sepClass = compiler.compile(fluxtionCompilerConfig, eventProcessorConfig);
        return fluxtionCompilerConfig.isCompileSource() ? sepClass.getDeclaredConstructor().newInstance() : null;
    }

    private static class InProcessEventProcessorConfig extends EventProcessorConfig {

        private final Consumer<EventProcessorConfig> cfg;

        public InProcessEventProcessorConfig(Consumer<EventProcessorConfig> cfg) {
            this.cfg = cfg;
        }

        @Override
        public void buildConfig() {
            cfg.accept(this);
        }
    }
}
