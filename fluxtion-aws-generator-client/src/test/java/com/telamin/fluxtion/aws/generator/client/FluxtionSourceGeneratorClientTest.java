package com.telamin.fluxtion.aws.generator.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.telamin.fluxtion.builder.compile.config.FluxtionCompilerConfig;
import com.telamin.fluxtion.builder.generation.config.EventProcessorConfig;
import com.telamin.fluxtion.builder.generation.model.EventProcessorModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

class FluxtionSourceGeneratorClientTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
        System.clearProperty("fluxtion.aws.apiBaseUrl");
        System.clearProperty("fluxtion.aws.apiKey");
        System.clearProperty("fluxtion.aws.zip");
    }

    @Test
    void writesReturnedSourceToWriter() throws Exception {
        server.createContext("/generation", new JsonResponder(201,
                "{\"contentType\":\"text/plain; charset=utf-8\",\"data\":\"public class Demo {}\"}"));
        System.setProperty("fluxtion.aws.apiBaseUrl", "http://127.0.0.1:" + port);
        System.setProperty("fluxtion.aws.apiKey", "ALICE");
        System.setProperty("fluxtion.aws.zip", "false");

        FluxtionSourceGeneratorClient gen = new FluxtionSourceGeneratorClient();
        StringWriter writer = new StringWriter();
        gen.generateDataFlowSource(null, new EventProcessorConfig(), new FluxtionCompilerConfig(), writer);
        String s = writer.toString();
        Assertions.assertTrue(s.contains("public class Demo"));
    }

    static class JsonResponder implements HttpHandler {
        private final int status;
        private final String body;
        JsonResponder(int status, String body) { this.status = status; this.body = body; }
        @Override public void handle(HttpExchange exchange) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }
}
