package com.telamin.fluxtion.builder.example;


import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Aggregates;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TutorialPart1 {
    record Trade(String symbol, int qty) {
    }

    public static void main(String[] args) {
        System.out.println("Building DataFlow: per-symbol net quantity over last N events");

        // A simple flow: subscribe -> filter -> map -> aggregate -> sink
        DataFlow flow = DataFlowBuilder
                .subscribe(Trade.class)
                .filter(t -> t.qty() != 0)                   // ignore zero-qty noise
                .map(t -> new Trade(t.symbol().toLowerCase(), t.qty() * 10)) // (symbol, qty)
                .groupBy(
                        Trade::symbol,                       // key: symbol
                        Trade::qty,                          // value: qty
                        Aggregates.intSumFactory())          // aggregator: running sum per key
                .map(GroupBy::toMap)                         // emit Map<symbol, Integer>
                .sink("netPosition")                         // named sink
                .build();

        // Register a sink to print updates
        flow.addSink("netPosition", System.out::println);

        // Drive some events periodically
        var exec = Executors.newSingleThreadScheduledExecutor();
        var counter = new AtomicInteger();
        exec.scheduleAtFixedRate(() -> {
            int i = counter.incrementAndGet();
            String sym = switch (i % 3) {
                case 0 -> "AAPL";
                case 1 -> "MSFT";
                default -> "GOOG";
            };
            int qty = (i % 2 == 0 ? +10 : -5); // alternate buy/sell
            flow.onEvent(new Trade(sym, qty));
        }, 100, 300, TimeUnit.MILLISECONDS);

        System.out.println("Publishing demo trades every 300 ms... Press Ctrl+C to stop\n");
    }
}
