# Tutorial Part‑4 — Package and embed in a microservice

In this tutorial you will:

- Embed a DataFlow in a tiny microservice.
- Add basic logging, a health check endpoint, and simple metrics hooks.
- Package and run it as a plain Java app.

## Prerequisites

- JDK 21+
- Maven (wrapper provided) or JBang for a quick run

## What we’ll build

- A simple service that receives synthetic events on a scheduler, computes a rolling metric, logs outputs, and exposes:
    - GET /health — reports ready
    - GET /metrics — returns a few counters in text

## Option A — Run with JBang (single file demo)

1. Create a file TutorialPart4.java with the code below.

```console
vi TutorialPart4.java
```
2. Run with jBang

```console 
jbang TutorialPart4.java 
```

```java
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_version}}
//DEPS org.slf4j:slf4j-simple:2.0.16
//JAVA 25

import com.sun.net.httpserver.HttpServer;
import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.builder.flowfunction.FlowBuilder;
import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive.IntAverageFlowFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TutorialPart4 {
    private static final Logger LOG = LoggerFactory.getLogger(TutorialPart4.class);

    record Request(int latencyMs) {
    }

    public static void main(String[] args) throws Exception {
        LOG.info("Starting microservice with embedded DataFlow");

        // Metrics
        AtomicLong eventsIn = new AtomicLong();
        AtomicLong alertsOut = new AtomicLong();
        AtomicLong avgLatency = new AtomicLong();

        // Build a DataFlow computing rolling average latency
        // 5s window, 1s buckets
        DataFlow flow = DataFlowBuilder
                .subscribe(Request.class)
                .map(Request::latencyMs)
                .slidingAggregate(IntAverageFlowFunction::new, 1000, 5)
                .sink("avgLatency")
                .map(avg -> avg > 250 ? "ALERT: high avg latency " + avg + "ms" : "data:" + avg + "ms")
                .sink("alerts")
                .build();

        // Wire sinks to logging/metrics
        flow.addSink("avgLatency", (Number avg) -> {
            avgLatency.set(avg.longValue());
            LOG.info("avgLatency={}ms", avg);
        });
        flow.addSink("alerts", (String msg) -> {
            alertsOut.incrementAndGet();
            LOG.warn("{}", msg);
        });

        // Start a tiny HTTP server (health + metrics)
        HttpServer server = httpServer(8080, eventsIn, alertsOut, avgLatency);
        server.start();
        LOG.info("HTTP server started on http://localhost:8080");

        // Drive synthetic requests
        var exec = Executors.newSingleThreadScheduledExecutor();
        var rnd = new Random();
        exec.scheduleAtFixedRate(() -> {
            int latency = 100 + rnd.nextInt(300); // 100..399ms
            eventsIn.incrementAndGet();
            flow.onEvent(new Request(latency));
        }, 100, 200, TimeUnit.MILLISECONDS);

        LOG.info("Service running. Try: curl -s localhost:8080/health | jq, curl -s localhost:8080/metrics");
    }

    private static HttpServer httpServer(int port, AtomicLong in, AtomicLong alerts, AtomicLong avg) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", exchange -> {
            String body = "{\"status\":\"UP\",\"time\":\"" + Instant.now() + "\"}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.createContext("/metrics", exchange -> {
            String body = "events_in " + in.get() + "\n" +
                    "alerts_out " + alerts.get() + "\n" +
                    "avg_latency_ms " + avg.get() + "\n";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; version=0.0.4");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        return server;
    }
}
```

## Option B — Maven project

- Add dependencies and a main class similar to the above. Recommended POM fragments:

```xml

<dependency>
    <groupId>com.telamin.fluxtion</groupId>
    <artifactId>fluxtion-builder</artifactId>
    <version>0.9.4</version>
</dependency>
<dependency>
<groupId>org.slf4j</groupId>
<artifactId>slf4j-simple</artifactId>
<version>2.0.16</version>
<scope>runtime</scope>
</dependency>
```

- To create a single runnable JAR, you can use Maven shade or your preferred packager. Example (optional):

```xml

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.5.0</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <createDependencyReducedPom>true</createDependencyReducedPom>
                        <transformers>
                            <transformer
                                    implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                <mainClass>com.example.TutorialPart4</mainClass>
                            </transformer>
                        </transformers>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## What you should see

- Print metrics and health check endpoints.
- Console logs for avg latency and any high‑latency alerts

```console
fluxtion-exmples % jbang TutorialPart4.java 
[jbang] Resolving dependencies...
[jbang]    com.telamin.fluxtion:fluxtion-builder:0.9.6
[jbang]    org.slf4j:slf4j-simple:2.0.16
[jbang] Dependencies resolved
[jbang] Building jar for TutorialPart4.java...
[main] INFO TutorialPart4 - Starting microservice with embedded DataFlow
[main] INFO TutorialPart4 - HTTP server started on http://localhost:8080
[main] INFO TutorialPart4 - Service running. Try: curl -s localhost:8080/health | jq, curl -s localhost:8080/metrics
[pool-1-thread-1] INFO TutorialPart4 - avgLatency=270ms
[pool-1-thread-1] WARN TutorialPart4 - ALERT: high avg latency 270ms
```

## How to verify

Start a new terminal and try to query the service REST endpoints with curl:

### Health check

REST query:
```console
curl -s localhost:8080/health
```

REST response:
```json
{"status":"UP","time":"2025-09-27T08:46:25.798373Z"}
```

### Metrics: 


REST query:
```console
curl -s localhost:8080/metrics
```

REST response:
```console
events_in 541
alerts_out 104
avg_latency_ms 276
```

## Key ideas reinforced

- Fluxtion is an embeddable library: no external server required.
- Sinks are a natural way to hook into logging and metrics.
- Simple HTTP endpoints can be added with JDK HttpServer; use your framework of choice in real services.

## Where to next

- Explore interpreted vs compiled graphs in Concepts and architecture.
- Add your own sinks for Prometheus, OpenTelemetry, or your logging framework.
- Repackage the flow as an AOT compiled graph for lower latency on critical paths.
