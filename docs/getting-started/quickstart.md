---
title: 1 minute tutorial
parent: DataFlow
nav_order: 1
published: true
layout: default
---

# DataFlow developer quickstart
---

Quickstart tutorial to get developers up and running in 1 minute leveraging jbang. Calculates the average speed
of s stream of cars grouped by manufacturer in a sliding window of 2 seconds with a 500 millisecond bucket size.

### 1. Install jbang

Open a new terminal or command shell to install jbang

Linux/OSX/Windows/AIX Bash:

```console 
curl -Ls https://sh.jbang.dev | bash -s - app setup 
```

Windows Powershell:

```console 
iex "& { $(iwr -useb https://ps.jbang.dev) } app setup" 
```

Close this terminal

### 2. Copy the java example

Copy the DataFlow java example into local file GroupByWindowExample.java

Linux/OSX/Windows/AIX Bash:

```console 
vi GroupByWindowExample.java 
```

Windows Powershell:

```console 
notepad.exe GroupByWindowExample.java 
```

```java
//DEPS com.telamin.fluxtion:fluxtion-builder:0.9.4
//COMPILE_OPTIONS -proc:full
//JAVA 25

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Aggregates;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

record CarTracker(String make, double speed) {
}

static String[] makes = new String[]{"BMW", "Ford", "Honda", "Jeep", "VW"};

// Calculates the average speed by manufacturer 
// in a sliding window of 2 seconds with a 500-millisecond bucket size
public void main() {
    System.out.println("building DataFlow::avgSpeedByMake...");

    //build the DataFlow
    DataFlow avgSpeedByMake = DataFlowBuilder
            .subscribe(CarTracker.class)
            .groupBySliding(
                    CarTracker::make,                  //key
                    CarTracker::speed,                 //value
                    Aggregates.doubleAverageFactory(), //avg function per bucket
                    500, 4)                            //4 buckets 500 millis each
            .mapValues(v -> "avgSpeed-" + v.intValue() + " km/h")
            .map(GroupBy::toMap)
            .sink("average car speed")
            .build();

    //register an output sink with the DataFlow
    avgSpeedByMake.addSink("average car speed", System.out::println);

    Random random = new Random();

    //schedule a task to send random events to the DataFlow every 400 millis
    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
            () -> avgSpeedByMake.onEvent(
                    new CarTracker(
                            makes[random.nextInt(makes.length)],
                            random.nextDouble(100))),
            100, 400, TimeUnit.MILLISECONDS);

    System.out.println("publishing events every 400 millis to DataFlow...\n");
}
```

### 3. Run the example with JBang

In the same terminal execute the example

```console
jbang GroupByWindowExample.java
```

Console output:

```console
%> jbang GroupByWindowExample.java
[jbang] Resolving dependencies...
[jbang]    com.fluxtion.dataflow:dataflow-builder:1.0.0
[jbang] Dependencies resolved
[jbang] Building jar for GroupByWindowExample.java...
building DataFlow::avgSpeedByMake...
publishing events to DataFlow...

{VW=avgSpeed-92 km/h, Jeep=avgSpeed-70 km/h, Ford=avgSpeed-79 km/h, BMW=avgSpeed-42 km/h}
{Jeep=avgSpeed-70 km/h, BMW=avgSpeed-53 km/h}
{BMW=avgSpeed-54 km/h}
{VW=avgSpeed-68 km/h, BMW=avgSpeed-65 km/h}
{VW=avgSpeed-68 km/h, Ford=avgSpeed-15 km/h, BMW=avgSpeed-62 km/h}
{VW=avgSpeed-79 km/h, Ford=avgSpeed-15 km/h, BMW=avgSpeed-38 km/h}
{VW=avgSpeed-79 km/h, Jeep=avgSpeed-24 km/h, Ford=avgSpeed-15 km/h, BMW=avgSpeed-16 km/h}
```

## What this example does

- Defines a simple event type `CarTracker` with two fields: `make` (car manufacturer) and `speed` (km/h).
- Builds a Fluxtion DataFlow pipeline that:
    - Subscribes to `CarTracker` events.
    - Groups events by `make`.
    - Maintains a sliding time window of the last 2 seconds, split into 4 buckets of 500 ms each.
    - Computes the average speed per make within that sliding window.
    - Formats each average as a string like `avgSpeed-62 km/h` and publishes a `Map<String, String>` of
      `{make -> avgSpeed-XX km/h}` to a named sink.
- Starts a scheduled task that publishes a random `CarTracker` (random make, random speed 0–100) every 400 ms to the
  DataFlow.
- Prints the current per-make average speeds to the console each time the flow emits an update.

### How the average speed is calculated

- The core operator is `groupBySliding(...)`:
    - `CarTracker::make` is the key extractor (group by manufacturer).
    - `CarTracker::speed` is the numeric value being aggregated.
    - `Aggregates.doubleAverageFactory()` provides an average aggregator for each time bucket.
    - `500, 4` defines the windowing model: 4 buckets × 500 ms = a 2-second sliding window.

- Internals of the sliding window average (conceptual):
    - For each `make`, the flow maintains 4 time buckets. Each bucket holds the partial aggregate for events that
      arrived during its 500 ms interval, typically `sum` and `count`.
    - When a new `CarTracker` arrives for some `make`:
        - The event is routed to the current active 500 ms bucket for that `make`.
        - The bucket’s aggregator updates its state (e.g., `sum += speed`, `count += 1`).
    - The per-make windowed average is computed across all buckets in the last 2 seconds:
        - `windowSum = sum(bucket0.sum, bucket1.sum, bucket2.sum, bucket3.sum)`
        - `windowCount = sum(bucket0.count, bucket1.count, bucket2.count, bucket3.count)`
        - `avg = windowSum / max(1, windowCount)`
    - As time advances, the oldest bucket expires every 500 ms and is reset for reuse, ensuring the window always
      represents the most recent 2 seconds of data.

- Because events are generated every ~400 ms, multiple events can land in the same bucket, and successive outputs will
  reflect a rolling average that smoothly updates as new events arrive and old ones fall out of the 2-second window.

### End-to-end flow walkthrough

1. Build the pipeline:
    - `DataFlowBuilder.subscribe(CarTracker.class)` starts an event stream of `CarTracker`.
    - `.groupBySliding(...)` groups by `make` and maintains a 2 s sliding window using 4 × 500 ms buckets with an
      average aggregator per bucket.
    - `.mapValues(v -> "avgSpeed-" + v.intValue() + " km/h")` formats the numeric averages.
    - `.map(GroupBy::toMap)` converts grouped key/value results into a `Map<make, formattedAverage>`.
    - `.sink("average car speed")` declares a named output sink.
    - `.build()` builds the `DataFlow` graph.

2. Register the sink:
    - `avgSpeedByMake.addSink("average car speed", System.out::println)` prints updates to the console.

3. Publish events:
    - A scheduled executor emits a random `CarTracker` event every 400 ms.

4. Observe output:
    - Each emission prints a map of makes to their current sliding-window average speed, e.g.:
        - `{VW=avgSpeed-79 km/h, Jeep=avgSpeed-24 km/h, Ford=avgSpeed-15 km/h, BMW=avgSpeed-16 km/h}`

### Why buckets?

- Buckets allow efficient sliding-window aggregation:
    - Each bucket maintains compact state (sum, count) instead of individual events.
    - Rolling the window only requires adding the current bucket and removing the expired one, keeping updates O(1) per
      event and per tick.

### Key code lines to relate to the description

- `groupBySliding(CarTracker::make, CarTracker::speed, Aggregates.doubleAverageFactory(), 500, 4)`
    - Group by manufacturer.
    - Average of speeds per make.
    - Sliding window: 2 seconds total, 500 ms buckets.
- `mapValues(...)` then `toMap` and `sink(...)` drive the formatted output to the console.
- The `ScheduledExecutorService` feeds the stream with random test data so you can see the averages changing in real
  time.