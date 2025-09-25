package com.telamin.fluxtion.builder.example;

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Aggregates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class SlidingWindowExample {

    private static final Logger LOG = LoggerFactory.getLogger(TutorialPart4.class);

    record Request(int latencyMs) {
    }

    public static void main(String[] args) {
        LOG.info("Starting microservice with embedded DataFlow");

        // Metrics
        AtomicLong eventsIn = new AtomicLong();
        AtomicLong alertsOut = new AtomicLong();
        AtomicLong avgLatency = new AtomicLong();

        //build dataflow
        DataFlow windowedFlow = DataFlowBuilder
                .subscribe(Request::latencyMs)
                .console("event in:{}")
                .slidingAggregate(Aggregates.intAverageFactory(), 1000, 2)
                .console("current sliding second sum:{} eventTime:%t")
                .id("sum")
                .sink("avgLatency")
                .map(avg -> avg > 250 ? "ALERT: high avg latency " + avg + "ms" : null)
                .sink("alerts")
                .build();

        windowedFlow.addSink("avgLatency", System.out::println);

        windowedFlow.addSink("avgLatency", (Number avg) -> {
//            avgLatency.set(avg.longValue());
            LOG.info("avgLatency={}ms", avg);
        });


        windowedFlow.addSink("alerts", (String msg) -> {
//            alertsOut.incrementAndGet();
            LOG.warn("{}", msg);
        });

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        var rnd = new Random();
        executor.scheduleAtFixedRate(() -> {
            int latency = 100 + rnd.nextInt(300); // 100..399ms
            windowedFlow.onEvent(new Request(latency));
//            windowedFlow.onEvent(latency);
        }, 100, 200, TimeUnit.MILLISECONDS);
    }


}
