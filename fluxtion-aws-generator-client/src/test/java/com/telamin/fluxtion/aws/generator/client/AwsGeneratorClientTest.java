package com.telamin.fluxtion.aws.generator.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

class AwsGeneratorClientTest {

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
    }

    @Test
    void decodesPlainTextResponse() throws Exception {
        server.createContext("/generation", new JsonResponder(201,
                "{\"contentType\":\"text/plain; charset=utf-8\",\"data\":\"Hello World\"}"));
        AwsGeneratorClient client = new AwsGeneratorClient("http://127.0.0.1:" + port, "test-key");
        byte[] out = client.postGeneration("ignored".getBytes(StandardCharsets.UTF_8), false);
        Assertions.assertEquals("Hello World", new String(out));
    }

    @Test
    void decodesBase64Response() throws Exception {
        String payload = Base64.getEncoder().encodeToString("Hi".getBytes(StandardCharsets.UTF_8));
        server.createContext("/generation", new JsonResponder(201,
                "{\"contentType\":\"application/zip\",\"encoding\":\"base64\",\"data\":\"" + payload + "\"}"));
        AwsGeneratorClient client = new AwsGeneratorClient("http://127.0.0.1:" + port, null);
        byte[] out = client.postGeneration("ignored".getBytes(StandardCharsets.UTF_8), true);
        Assertions.assertEquals("Hi", new String(out));
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
