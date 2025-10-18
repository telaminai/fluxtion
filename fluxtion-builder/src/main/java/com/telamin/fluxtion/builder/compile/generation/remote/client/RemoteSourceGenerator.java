/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.compile.generation.remote.client;

import com.telamin.fluxtion.builder.compile.config.FluxtionCompilerConfig;
import com.telamin.fluxtion.builder.compile.generation.SourceGenerator;
import com.telamin.fluxtion.builder.compile.generation.remote.common.RemoteGenerationRequest;
import com.telamin.fluxtion.builder.compile.generation.remote.common.RemoteGenerationResponse;
import com.telamin.fluxtion.builder.generation.config.EventProcessorConfig;
import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import com.telamin.fluxtion.builder.generation.model.EventProcessorModel;

import java.io.*;
import java.net.Socket;

public class RemoteSourceGenerator implements SourceGenerator {

    public static final String SERVICE_ID = "remote";
    public static final String PROP_HOST = "fluxtion.remote.host";
    public static final String PROP_PORT = "fluxtion.remote.port";

    @Override
    public String id() {
        return SERVICE_ID;
    }

    @Override
    public void generateDataFlowSource(EventProcessorModel simpleEventProcessorModel,
                                       EventProcessorConfig config,
                                       FluxtionCompilerConfig compilerConfig,
                                       Writer templateWriter) throws Exception {
        String host = System.getProperty(PROP_HOST, "127.0.0.1");
        int port = Integer.getInteger(PROP_PORT, 7070);

        RemoteGenerationRequest req = new RemoteGenerationRequest();
        req.setModel(simpleEventProcessorModel);
        req.setInlineEventHandling(config.isInlineEventHandling());
        req.setSupportBufferAndTrigger(config.isSupportBufferAndTrigger());
        req.setDispatchStrategy(config.getDispatchStrategy());
        req.setTemplateFile(config.getTemplateFile());
        req.setInterfacesToImplement(config.interfacesToImplement());
        req.setCompilerConfig(compilerConfig);
        req.setPackageName(GenerationContext.SINGLETON.getPackageName());
        req.setClassName(GenerationContext.SINGLETON.getSepClassName());

        RemoteGenerationResponse resp = invoke(host, port, req);
        if (!resp.isSuccess()) {
            throw new IllegalStateException("Remote generation failed: " + resp.getError());
        }
        templateWriter.write(resp.getSource());
        templateWriter.flush();
        templateWriter.close();
    }

    private RemoteGenerationResponse invoke(String host, int port, RemoteGenerationRequest req) throws IOException, ClassNotFoundException {
        try (Socket socket = new Socket(host, port)) {
            // Create ObjectOutputStream first to send header, write request, then create ObjectInputStream to read response.
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            oos.writeObject(req);
            oos.flush();

            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
            Object resp = ois.readObject();
            return (RemoteGenerationResponse) resp;
        }
    }
}
