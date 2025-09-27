# Tutorial Part‑1 — Build your first DataFlow

In this hands‑on tutorial you will:

- Build a tiny DataFlow that filters, maps, and aggregates.
- Run it in‑process and print results to the console.
- Understand at‑most‑once dispatch and what triggers recomputation.

## Prerequisites

- JDK 21+
- Maven Wrapper (provided in this repo) or JBang

## Option A — Run with JBang (fastest path)

1. Create a file TutorialPart1.java with the code below.

```console
vi TutorialPart1.java
```
2. Run with jBang

```console 
jbang TutorialPart1.java 
```

```java
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_version}}
//JAVA 25

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
                .sink("trades")
                .map(t -> new Trade(
                        t.symbol().toLowerCase(), 
                        t.qty() * 10))                      // Map - symbol, qty
                .groupBy(
                        Trade::symbol,                       // key: symbol
                        Trade::qty,                          // value: qty
                        Aggregates.intSumFactory())          // aggregator: running sum per key
                .map(GroupBy::toMap)                         // emit Map<symbol, Integer>
                .sink("netPosition")                         // named sink
                .build();

        // Register a sink to print updates
        flow.addSink("netPosition", System.out::println);
        flow.addSink("trades", System.out::println);

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
            System.out.println("");
        }, 100, 300, TimeUnit.MILLISECONDS);

        System.out.println("Publishing demo trades every 300 ms... Press Ctrl+C to stop\n");
    }
}
```

## Option B — Add to a Maven project

- Dependency (use the version from this repo’s docs homepage if newer):

```xml

<dependency>
    <groupId>com.telamin.fluxtion</groupId>
    <artifactId>fluxtion-builder</artifactId>
    <version>0.9.4</version>
</dependency>
```

- Then add the TutorialPart1 class above to your sources and run it from your IDE.

## What you should see

- Console prints a map of net positions that changes incrementally, for example:
```console
fluxtion-exmples % jbang TutorialPart1.java
Building DataFlow: per-symbol net quantity over last N events
Publishing demo trades every 300 ms... Press Ctrl+C to stop

Trade[symbol=MSFT, qty=-5]
{msft=-50}

Trade[symbol=GOOG, qty=10]
{goog=100, msft=-50}

Trade[symbol=AAPL, qty=-5]
{goog=100, aapl=-50, msft=-50}

Trade[symbol=MSFT, qty=10]
```

- Only affected keys change as new trades arrive.

## How it works (relate to Concepts page)

- DAG and dispatch: Each Trade triggers a single topological pass. Nodes are invoked at most once per event.
- Triggering recomputation: A new Trade updates the group for its symbol; only that key’s aggregate changes, then the
  sink fires.
- Deterministic order: filter → map → groupBy → toMap → sink every time.

## Next steps

- Read Concepts and architecture to internalize the DAG and execution model.
- Try the 1 minute tutorial for a sliding‑window average.
- Part‑2 (coming next): windowed metrics and joining multiple streams.
