package com.telamin.fluxtion.builder.example;

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
//        FlowBuilder<Integer> sliding5SecondAvg = DataFlowBuilder
//                .subscribe(Request.class)
//                .map(Request::latencyMs)
//                .slidingAggregate(IntAverageFlowFunction::new, 1000, 5);
//
//        // send to sink avg latency
//        sliding5SecondAvg.sink("avgLatency");

//        sliding5SecondAvg
//                .map(x-> {
//                    System.out.println("avg latency:" + x);
//                    return x;
//                })
//                .console("current avg latency: {}ms");

        // send to sink alerts
        DataFlow flow = DataFlowBuilder
                .subscribe(Request.class)
                .map(Request::latencyMs)
                .slidingAggregate(IntAverageFlowFunction::new, 1000, 5)
                .map(avg -> avg > 250 ? "ALERT: high avg latency " + avg + "ms" : "data:" + avg + "ms")
//                .filter(msg -> msg != null)
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

        flow.start();

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
