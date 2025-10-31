/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.aws.generator.client;

import com.telamin.fluxtion.builder.compile.config.FluxtionCompilerConfig;
import com.telamin.fluxtion.builder.compile.generation.SourceGenerator;
import com.telamin.fluxtion.builder.compile.generation.remote.common.RemoteGenerationRequest;
import com.telamin.fluxtion.builder.generation.config.EventProcessorConfig;
import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import com.telamin.fluxtion.builder.generation.model.EventProcessorModel;

import java.io.*;

/**
 * SourceGenerator implementation that calls the AWS-hosted REST API (or SAM local) to generate source code.
 *
 * System properties:
 *  - fluxtion.aws.apiBaseUrl (required): Base URL of the API, e.g., http://127.0.0.1:3000
 *  - fluxtion.aws.apiKey (optional): API key/JWT for Authorization header
 *  - fluxtion.aws.zip (optional): if true, request zipped output (base64-encoded)
 */
public class FluxtionSourceGeneratorClient implements SourceGenerator {
    public static final String SERVICE_ID = "aws-http";

    @Override
    public String id() {
        return SERVICE_ID;
    }

    @Override
    public void generateDataFlowSource(EventProcessorModel simpleEventProcessorModel,
                                       EventProcessorConfig config,
                                       FluxtionCompilerConfig compilerConfig,
                                       Writer templateWriter) throws Exception {
        String baseUrl = System.getProperty("fluxtion.aws.apiBaseUrl", "http://127.0.0.1:3000");
        String apiKey = System.getProperty("fluxtion.aws.apiKey", "");
        boolean zip = Boolean.parseBoolean(System.getProperty("fluxtion.aws.zip", "false"));
        AwsGeneratorClient client = new AwsGeneratorClient(baseUrl, apiKey);

        RemoteGenerationRequest req = new RemoteGenerationRequest();
        req.setModel(simpleEventProcessorModel);
        req.setInlineEventHandling(config.isInlineEventHandling());
        req.setSupportBufferAndTrigger(config.isSupportBufferAndTrigger());
        req.setDispatchStrategy(config.getDispatchStrategy());
        req.setTemplateFile(config.getTemplateFile());
        req.setInterfacesToImplement(config.interfacesToImplement());
        req.setCompilerConfig(compilerConfig);
        String pkg = (GenerationContext.SINGLETON == null || GenerationContext.SINGLETON.getPackageName() == null)
                ? "generated" : GenerationContext.SINGLETON.getPackageName();
        String cls = (GenerationContext.SINGLETON == null || GenerationContext.SINGLETON.getSepClassName() == null)
                ? "Demo" : GenerationContext.SINGLETON.getSepClassName();
        req.setPackageName(pkg);
        req.setClassName(cls);

        byte[] payload = serialize(req);
        byte[] response = client.postGeneration(payload, zip);
        if (zip) {
            // For now, if zipped, write a stub note; callers wanting zip should use the download endpoint.
            templateWriter.write("// Received zipped content (not expanded here)\n");
        } else {
            templateWriter.write(new String(response));
        }
        templateWriter.flush();
        templateWriter.close();
    }

    private static byte[] serialize(Serializable obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }
}
