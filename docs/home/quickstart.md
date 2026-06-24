# Fluxtion Quickstart

This guide introduces Fluxtion through a series of progressively more capable examples. Each example adds new behaviour
while the amount of orchestration code stays almost constant.

Fluxtion infers the execution pipeline from the relationships between components at compile time, so developers only 
write the business logic.

---

## Install JBang

Examples use **JBang** so they can run without creating a full project.

Install JBang:

```bash
# macOS / Linux
curl -Ls https://sh.jbang.dev | bash

# Windows (powershell)
iex "& { $(iwr https://ps.jbang.dev) }"
```

More details: [https://www.jbang.dev](https://www.jbang.dev)

---

## 1. Hello world

Create a simple event pipeline.

```java
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_version}}
//JAVA 25

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.runtime.DataFlow;

void main() {
    DataFlow dataFlow = DataFlowBuilder
            .subscribe(String.class)
            .map(String::toUpperCase)
            .console("msg:{}")
            .build();

    dataFlow.onEvent("hello");
    dataFlow.onEvent("world");
}
```

Run:

```bash
jbang hello.java
```

Output:

```text
msg:HELLO
msg:WORLD
```
Fluxtion automatically determines the execution order for the pipeline and generates the runtime event processor.

---

## 2. Streaming analytics

Compute a sliding window average from a real-time event stream.

```java
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_version}}
//JAVA 25

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Aggregates;

import java.util.Random;

record CarTracker(String id, double speed) { }

void main() throws InterruptedException {
    // 5 buckets of 200ms = 1 second sliding window
    DataFlow averageCarSpeed = DataFlowBuilder
            .subscribe(CarTracker::speed)
            .slidingAggregate(Aggregates.doubleAverageFactory(), 200, 5)
            .map(v -> "average speed: " + v.intValue() + " km/h")
            .sink("average car speed")
            .build();

    averageCarSpeed.addSink("average car speed", System.out::println);

    Random random = new Random();

    for (int i = 0; i < 100; i++) {
        averageCarSpeed.onEvent(
                new CarTracker("car-" + (i % 5), random.nextInt(100)));
        Thread.sleep(random.nextInt(100));
    }
}

```

Run:

```bash
jbang streamingAnalytics.java
```

Output:

```text
average speed: 45 km/h
average speed: 41 km/h
average speed: 42 km/h
average speed: 45 km/h
average speed: 47 km/h
average speed: 55 km/h
average speed: 58 km/h
average speed: 63 km/h
average speed: 61 km/h
average speed: 56 km/h
average speed: 55 km/h

```

Fluxtion maintains the sliding window and automatically triggers downstream calculations.

---

## 3. Stateful node example

Fluxtion integrates directly with ordinary Java objects.

```java
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_version}}
//JAVA 25

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.annotations.OnEventHandler;

void main() {
    DataFlow flow = DataFlowBuilder
            .subscribeToNode(new StatefulNode())
            .map(StatefulNode::getValue)
            .console("value:{}")
            .build();

    flow.onEvent("A");
    flow.onEvent("B");
    flow.onEvent("C");
    flow.onEvent("D");
    flow.onEvent("E");
    flow.onEvent("F");
}

public class StatefulNode {

    private String value;

    @OnEventHandler
    public boolean update(String input) {
        value = input;
        return true;
    }

    public String getValue() {
        return value;
    }
}
```

Run:

```bash
jbang statefulNode.java
```

Output:

```text
value:A
value:B
value:C
value:D
value:E
value:F

```

Stateful objects participate directly in the event graph.

---

## 4. Multi‑feed event system

Real systems often combine multiple event streams. Fluxtion can also combine multiple event streams using joins.

```java
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_version}}
//JAVA 25

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.builder.flowfunction.JoinFlowBuilder;
import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Tuples;

import java.util.List;
import java.util.stream.Collectors;

public record Pupil(int year, String school, String name) {
}

public record School(String name) {
}

void main(String[] args) {
    var pupils = DataFlowBuilder.subscribe(Pupil.class).groupByToList(Pupil::school);
    var schools = DataFlowBuilder.subscribe(School.class).groupBy(School::name);

    DataFlow processor = JoinFlowBuilder
            .innerJoin(schools, pupils)
            .mapValues(Tuples.mapTuple(Helper::prettyPrint))
            .map(GroupBy::toMap)
            .console()
            .build();

    //register some schools
    processor.onEvent(new School("RGS"));
    processor.onEvent(new School("Belles"));

    //register some pupils
    processor.onEvent(new Pupil(2015, "RGS", "Bob"));
    processor.onEvent(new Pupil(2013, "RGS", "Ashkay"));
    processor.onEvent(new Pupil(2013, "Belles", "Channing"));
    processor.onEvent(new Pupil(2013, "RGS", "Chelsea"));
    processor.onEvent(new Pupil(2013, "Belles", "Tamsin"));
    processor.onEvent(new Pupil(2013, "Belles", "Ayola"));
    processor.onEvent(new Pupil(2015, "Belles", "Sunita"));
}

public static class Helper{
    public static String prettyPrint(School schoolName, List<Pupil> pupils) {
        return pupils.stream().map(Pupil::name).collect(Collectors.joining(",", "pupils[", "]"));
    }
}
```

Run:

```bash
jbang multiJoin.java
```

Output:

```text
{}
{}
{RGS=pupils[Bob]}
{RGS=pupils[Bob,Ashkay]}
{Belles=pupils[Channing], RGS=pupils[Bob,Ashkay]}
{Belles=pupils[Channing], RGS=pupils[Bob,Ashkay,Chelsea]}
{Belles=pupils[Channing,Tamsin], RGS=pupils[Bob,Ashkay,Chelsea]}
{Belles=pupils[Channing,Tamsin,Ayola], RGS=pupils[Bob,Ashkay,Chelsea]}
{Belles=pupils[Channing,Tamsin,Ayola,Sunita], RGS=pupils[Bob,Ashkay,Chelsea]}

```

Fluxtion composes multiple streams while maintaining deterministic execution.

---

## 5. Robotics / defence example

Fluxtion is well suited for deterministic control systems such as drones or robotics.

This example reacts to altitude events and triggers a safety warning.

```java
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_version}}
//JAVA 25

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.runtime.DataFlow;

record Altitude(double meters) {
}


void main(String[] args) {
    DataFlow droneSafety = DataFlowBuilder
            .subscribe(Altitude::meters)
            .filter(a -> a > 120)
            .map(a -> "WARNING: altitude limit exceeded " + a)
            .console("{}")
            .build();

    // sensor events
    droneSafety.onEvent(new Altitude(80));
    droneSafety.onEvent(new Altitude(95));
    droneSafety.onEvent(new Altitude(140));
}
```

Run:

```bash
jbang altitudeMonitor.java
```

Output:

```text
WARNING: altitude limit exceeded 140.0

```

Because the execution graph is deterministic, behaviour can be analysed, tested, and replayed — an important property 
for safety-critical systems.

---

# Next steps

Now that you have seen how Fluxtion coordinates logic, explore how to run it in a production runtime:

*   **[Run Fluxtion in Mongoose](../getting-started/run-fluxtion-in-mongoose.md)**: Host your processor in a managed runtime with feeds and threads.
*   **[Deterministic Replay](../how-to/replay-functionality.md)**: Exactly reproduce scenarios by replaying input streams.
*   **[Performance Results](../reference/performance.md)**: See the low-latency benchmarks for compiled DataFlows.

See the **[Why Fluxtion exists](why-fluxtion.md)** guide for a deeper look at the architecture.
