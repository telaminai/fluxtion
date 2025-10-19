/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.compile.generation;

import com.telamin.fluxtion.builder.compile.config.FluxtionCompilerConfig;
import com.telamin.fluxtion.builder.compile.exporter.JgraphGraphMLExporter;
import com.telamin.fluxtion.builder.compile.exporter.PngGenerator;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.telamin.fluxtion.builder.generation.config.EventProcessorConfig;
import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import com.telamin.fluxtion.builder.generation.model.SimpleEventProcessorModel;
import com.telamin.fluxtion.builder.generation.model.TopologicallySortedDependencyGraph;
import com.telamin.fluxtion.builder.generation.util.ClassUtils;
import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.annotations.ExportService;
import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import com.telamin.fluxtion.runtime.event.Event;
import com.telamin.fluxtion.runtime.node.EventHandlerNode;
import org.jgrapht.ext.IntegerEdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerConfigurationException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Greg Higgins
 */
public class EventProcessorGenerator {

    public static final String SOURCE_GENERATOR_ID_PROPERTY = "fluxtion.sourceGeneratorId";
    private EventProcessorConfig config;
    private static final Logger LOG = LoggerFactory.getLogger(EventProcessorGenerator.class);
    private SimpleEventProcessorModel simpleEventProcessorModel;
    private FluxtionCompilerConfig compilerConfig;


    public void generateDataFlowSource(EventProcessorConfig config, FluxtionCompilerConfig compilerConfig, Writer writer) throws Exception {
        try (ExecutorService execSvc = Executors.newCachedThreadPool()) {
            config.buildConfig();
            this.config = config;
            this.compilerConfig = compilerConfig;
            LOG.debug("start graph calc");
            GenerationContext context = GenerationContext.SINGLETON;
            //generate model
            TopologicallySortedDependencyGraph graph = new TopologicallySortedDependencyGraph(
                    config.getNodeList(),
                    config.getPublicNodes(),
                    config.getNodeFactoryRegistration(),
                    context,
                    config.getAuditorMap(),
                    config
            );
            LOG.debug("start model gen");
            simpleEventProcessorModel = new SimpleEventProcessorModel(graph, config.getFilterMap(), context.getProxyClassMap());
            simpleEventProcessorModel.setDispatchOnlyVersion(compilerConfig.isDispatchOnlyVersion());
            simpleEventProcessorModel.generateMetaModel(config.isSupportDirtyFiltering());
            //TODO add conditionality for different target languages
            //buildJava output
            if (compilerConfig.isGenerateDescription()) {
                execSvc.submit(() -> {
                    LOG.debug("start exporting graphML/images");
                    exportGraphMl(graph);
                    LOG.debug("completed exporting graphML/images");
                    LOG.debug("finished generating SEP");
                });
            }
            LOG.debug("start template output");
            generateDataFlowSource(writer);
            LOG.debug("completed template output");
            execSvc.shutdown();
            execSvc.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    public SimpleEventProcessorModel getSimpleEventProcessorModel() {
        return simpleEventProcessorModel;
    }


    private void generateDataFlowSource(Writer templateWriter) throws Exception {
        SourceGenerator generator = loadSourceGenerator();
        generator.generateDataFlowSource(simpleEventProcessorModel, config, compilerConfig, templateWriter);
    }

    private SourceGenerator loadSourceGenerator() {
        String desiredId = System.getProperty(SOURCE_GENERATOR_ID_PROPERTY, "").trim();
        ServiceLoader<SourceGenerator> loader;
        ClassLoader cl = GenerationContext.SINGLETON != null ? GenerationContext.SINGLETON.getClassLoader() : this.getClass().getClassLoader();
        try {
            loader = ServiceLoader.load(SourceGenerator.class);//, cl);
        } catch (Throwable t) {
            LOG.warn("Failed to load SourceGenerator via ServiceLoader with classloader: {}. Error: {}", cl, t.toString());
            throw new IllegalStateException("Failed to load SourceGenerator via ServiceLoader with classloader: " + cl, t);
        }
        SourceGenerator first = null;
        for (SourceGenerator sg : loader) {
            if (first == null) first = sg;
            if (!desiredId.isEmpty() && desiredId.equalsIgnoreCase(sg.id())) {
                LOG.debug("Selected SourceGenerator by id '{}' -> {}", desiredId, sg.getClass().getName());
                return sg;
            }
        }
        if (!desiredId.isEmpty()) {
            throw new IllegalStateException("No SourceGenerator found with id '" + desiredId + "'. Set system property '" + SOURCE_GENERATOR_ID_PROPERTY + "' to a valid id or ensure the implementation is on the classpath and registered.");
        }
        if (first != null) {
            LOG.debug("Selected default SourceGenerator -> {} (id='{}')", first.getClass().getName(), first.id());
            return first;
        }
        LOG.warn("No SourceGenerator providers discovered via ServiceLoader, falling back to VelocityGenerator");
        throw new IllegalStateException("No SourceGenerator providers discovered via ServiceLoader");
    }

    public static void formatSource(File outFile) {
        try {
            LOG.debug("Reading source:'{}'", outFile.getCanonicalPath());
            CharSource source = Files.asCharSource(outFile, Charset.defaultCharset());
            CharSink output = Files.asCharSink(outFile, Charset.defaultCharset());
            LOG.debug("formatting source - start");
            new Formatter().formatSource(source, output);
            LOG.debug("formatting source - finish");
        } catch (FormatterException | IOException ex) {
            LOG.error("problem formatting source file", ex);
        }
    }

    private void exportGraphMl(TopologicallySortedDependencyGraph graph) {
        try {
            LOG.debug("generating event images and graphml");
            File graphMl = new File(GenerationContext.SINGLETON.getResourcesOutputDirectory(), GenerationContext.SINGLETON.getSepClassName() + ".graphml");
            File pngFile = new File(GenerationContext.SINGLETON.getResourcesOutputDirectory(), GenerationContext.SINGLETON.getSepClassName() + ".png");
            if (graphMl.getParentFile() != null) {
                graphMl.getParentFile().mkdirs();
            }
            try (FileWriter graphMlWriter = new FileWriter(graphMl)) {
                exportAsGraphMl(graph, graphMlWriter, true);
            }
            PngGenerator.generatePNG(graphMl, pngFile);
        } catch (IOException | TransformerConfigurationException | SAXException iOException) {
            LOG.error("error writing png and graphml:", iOException);
        }
    }

    public static void exportAsGraphMl(TopologicallySortedDependencyGraph topoGraph, Writer writer, boolean addEvents) throws SAXException, TransformerConfigurationException {
        //graphml representation
        VertexNameProvider<Object> np = vertex -> {
            String name = topoGraph.variableName(vertex);
            if (name == null) {
                name = ((Class<?>) vertex).getSimpleName();
            }
            return name;
        };

        SimpleDirectedGraph<Object, DefaultEdge> graph = topoGraph.getGraph();

        JgraphGraphMLExporter<Object, Object> mlExporter = new JgraphGraphMLExporter<>(np, np,
                new IntegerEdgeNameProvider<>(), new IntegerEdgeNameProvider<>());
        @SuppressWarnings("unchecked") SimpleDirectedGraph<Object, Object> exportGraph = (SimpleDirectedGraph<Object, Object>) graph.clone();
        Set<Class<?>> exportServiceSet = new HashSet<>();
        if (addEvents) {
            graph.vertexSet().forEach((t) -> {
                Method[] methodList = t.getClass().getMethods();
                for (Method method : methodList) {
                    if (method.getAnnotation(OnEventHandler.class) != null) {
                        @SuppressWarnings("unchecked") Class<? extends Event> eventTypeClass = (Class<? extends Event>) method.getParameterTypes()[0];
                        exportGraph.addVertex(eventTypeClass);
                        exportGraph.addEdge(eventTypeClass, t);
                    }
                }
                if (t instanceof EventHandlerNode) {
                    EventHandlerNode<?> eh = (EventHandlerNode<?>) t;
                    Class<?> eventClass = eh.eventClass();
                    if (eventClass != null) {
                        exportGraph.addVertex(eventClass);
                        exportGraph.addEdge(eventClass, t);
                    }
                }
                if (t instanceof DataFlow) {
                    //TODO loop and add to the graph
                }
                for (AnnotatedType annotatedInterface : ClassUtils.getAllAnnotatedAnnotationTypes(t.getClass(), ExportService.class)) {
                    if (annotatedInterface.isAnnotationPresent(ExportService.class)) {
                        Class<?> interfaceType = (Class<?>) annotatedInterface.getType();
                        exportServiceSet.add(interfaceType);
                        exportGraph.addVertex(interfaceType);
                        exportGraph.addEdge(interfaceType, t);
                    }
                }
//                t.getClass().getInterfaces()
            });

//            pushEdges.stream()
//                    .filter(Objects::nonNull)
//                    .forEach((DefaultEdge edge) -> {
//                        Object edgeSource = graph.getEdgeSource(edge);
//                        Object edgeTarget = graph.getEdgeTarget(edge);
//                        exportGraph.removeEdge(edgeSource, edgeTarget);
//                        exportGraph.addEdge(edgeTarget, edgeSource);
//                    });

        }
        mlExporter.export(writer, exportGraph, exportServiceSet);//new EdgeReversedGraph(graph));
    }
}