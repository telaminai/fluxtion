package com.telamin.fluxtion.aws.generator.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

public class AwsGeneratorClient {
    private static final Logger LOG = LoggerFactory.getLogger(AwsGeneratorClient.class);

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient http;

    public AwsGeneratorClient(String baseUrl, String apiKey) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl").replaceAll("/$", "");
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    public byte[] postGeneration(byte[] javaSerializedRemoteGenerationRequest, boolean zip) throws IOException, InterruptedException {
        String url = baseUrl + "/generation" + (zip ? "?zip=true" : "");
        String bodyB64 = Base64.getEncoder().encodeToString(javaSerializedRemoteGenerationRequest);
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/octet-stream")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(bodyB64));
        if (apiKey != null && !apiKey.isBlank()) {
            req.header("Authorization", apiKey);
        }
        HttpResponse<String> resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            String s = resp.body();
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(s);
                String encoding = root.hasNonNull("encoding") ? root.get("encoding").asText("") : "";
                String data = root.hasNonNull("data") ? root.get("data").asText("") : "";
                if ("base64".equalsIgnoreCase(encoding)) {
                    return Base64.getDecoder().decode(data);
                } else {
                    // asText() returns unescaped content already
                    return data.getBytes(StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                LOG.warn("Failed to parse JSON response, returning raw body. error={}", e.toString());
                return s.getBytes(StandardCharsets.UTF_8);
            }
        }
        throw new IOException("Server returned status " + resp.statusCode() + ": " + resp.body());
    }
}
