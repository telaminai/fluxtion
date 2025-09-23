# Fluxtion

## Welcome to Fluxtion!

Fluxtion is a Java library and builder for real-time, in‑memory dataflow processing. It helps you express business logic
as a directed graph of small, composable functions and stateful components that update incrementally when new events
arrive.

Think of it like a spreadsheet for streams: when an input changes, only the dependent cells recompute. Fluxtion brings
this incremental, dependency-driven update model to event streams, enabling predictable, low‑latency processing with
clear dependencies and excellent performance.

## Highlights

- DataFlow programming model: declare how values depend on each other, not when to recompute.
- Deterministic execution: events propagate through a topologically sorted graph.
- Low overhead: avoids generic reactive machinery; directly invokes methods in dependency order.
- Scales down and up: great for microservices, trading systems, IoT gateways, or embedded analytics.
- Familiar Java: plain objects and methods; no special runtime server required.

## Core building blocks

- Events: any Java object submitted into the DataFlow.
- Nodes: functions or stateful components wired together by dependencies.
- Graph: a DAG computed by the builder that determines dispatch order.
- Handlers and triggers: annotated methods that receive events or fire when dependencies update.

## Where Fluxtion fits

- Real‑time analytics and monitoring
- Complex event processing and enrichment
- Per‑key aggregations (counts, moving windows)
- Signal generation and alerting
- Incremental computation pipelines

## Start here

- [Introduction](home/introduction.md)
- [Why Fluxtion](home/why-fluxtion.md)
- [What is DataFlow](home/what-is-dataflow.md)
- [For engineers](home/intro-engineers.md)
- [For managers](home/intro-managers.md)

## Quickstart

Install the builder (which depends on the runtime) and try a 2‑minute example.

Maven (pom.xml):

```xml
<dependency>
  <groupId>com.telamin.fluxtion</groupId>
  <artifactId>fluxtion-builder</artifactId>
  <version>0.9.3</version>
</dependency>
```

Gradle (Kotlin DSL):

```kotlin
implementation("com.telamin.fluxtion:fluxtion-builder:0.9.3")
```

Hello world (Java 21):

```java
import com.telamin.fluxtion.runtime.DataFlow;

public class HelloFluxtion {
    public static void main(String[] args) {
        DataFlow dataFlow = DataFlowBuilder
                .subscribe(String.class)           // accept String events
                .map(String::toUpperCase)          // transform
                .console("msg:{}")
                .build();// print to console

        dataFlow.onEvent("hello");  // prints: msg:HELLO
        dataFlow.onEvent("world");  // prints: msg:WORLD
    }
}
```

Run locally: see [Run the docs site locally](run_local_guide.md) and use your IDE to run the main above.

## Reference

- [DataFlow fundamentals](home/dataflow-fundamentals.md)
- [Reference guide](reference/reference-documentation.md)