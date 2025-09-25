# Tutorial Part‑2 — Per‑key aggregations, windows, and alerts

In this tutorial you will:

- Maintain per‑key metrics (e.g., per symbol) using groupBy.
- Use a sliding time window with buckets for rolling averages.
- Emit alerts when thresholds are breached.

Prerequisites

- JDK 21+
- JBang or Maven (see options below)

Option A — Run with JBang (fastest path)

1. Create a file TutorialPart2.java with the code below.
2. Run: jbang TutorialPart2.java

```java
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_version}}
//COMPILE_OPTIONS -proc:full
//JAVA 21

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.builder.flowfunction.GroupByFlowBuilder;
import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Aggregates;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TutorialPart2 {
    record Tick(String symbol, double price, long ts) {
    }

    public static void main(String[] args) {
        System.out.println("Building DataFlow: per-symbol sliding average and alerts");

        // 2-second sliding window, 4 buckets of 500ms
        var slidingTicker = DataFlowBuilder
                .subscribe(Tick.class)
                .groupBySliding(
                        Tick::symbol,                  // key per symbol
                        Tick::price,                   // numeric value
                        Aggregates.doubleAverageFactory(),
                        500, 4)                        // 4 buckets x 500ms = 2s window
                .mapValues(avg -> Math.round(avg * 100.0) / 100.0);

        // send to sink rolling averages
        slidingTicker
                .map(GroupBy::toMap)
                .sink("avgBySymbol");

        // send to sink alerts
        DataFlow flow = slidingTicker // round to 2dp
                .mapValues(avg -> avg > 150.0 ? "ALERT: avg>150" : null)
                .filterValues(msg -> msg != null)
                .map(GroupBy::toMap)
                .sink("alerts")
                .build();

        // Print rolling averages
        flow.addSink("avgBySymbol", (Map<String, Double> m) -> {
            System.out.println(Instant.now() + " avg=" + m);
        });
        // Print alerts
        flow.addSink("alerts", (Map<String, String> alerts) -> {
            if (!alerts.isEmpty()) {
                System.out.println(">>> ALERTS: " + alerts + "\n");
            }
        });

        // Drive random ticks for 10 seconds
        var exec = Executors.newSingleThreadScheduledExecutor();
        var rand = new Random();
        String[] syms = {"AAPL", "MSFT", "GOOG"};
        exec.scheduleAtFixedRate(() -> {
            String s = syms[rand.nextInt(syms.length)];
            // Random walk around ~140..160 to trigger alerts sometimes
            double price = 140 + rand.nextGaussian() * 10 + (rand.nextBoolean() ? 10 : 0);
            flow.onEvent(new Tick(s, price, System.currentTimeMillis()));
        }, 100, 200, TimeUnit.MILLISECONDS);

        System.out.println("Publishing random ticks every 200 ms. Watch averages and occasional alerts...\n");
    }
}
```

Option B — Maven

- Add the dependency:

```xml

<dependency>
    <groupId>com.telamin.fluxtion</groupId>
    <artifactId>fluxtion-builder</artifactId>
    <version>0.9.4</version>
</dependency>
```

- Add the TutorialPart2 class to your sources and run.

What you should see

- Console logs of per‑symbol averages changing as new ticks arrive.
- Occasional ALERT lines when a symbol’s 2‑second average exceeds 150.

Key ideas reinforced

- groupBySliding maintains a rolling window per key with fixed‑size time buckets.
- Mapping and tee allow you to fan out to multiple sinks (averages + alerts) from one computation.
- Only affected keys update, preserving incremental recomputation semantics.

Next steps

- Proceed to Part‑3 to combine the DSL with an imperative, stateful component and lifecycle callbacks.
