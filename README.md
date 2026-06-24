# Fluxtion

**Deterministic, low-latency continuous decision engine for Java with event replay.**

[![Maven Central](https://img.shields.io/maven-central/v/com.telamin.fluxtion/fluxtion-runtime.svg)](https://search.maven.org/search?q=g:com.telamin.fluxtion)
![Java](https://img.shields.io/badge/java-21+-blue)
[![License](https://img.shields.io/badge/license-AGPL%2FSSPL-important)](./LICENSE)
[![Docs](https://img.shields.io/badge/docs-website-blue)](https://telaminai.github.io/fluxtion/)

Fluxtion replaces reactive pipelines and event wiring with a compiled, deterministic execution engine. Instead of interpreting a runtime graph, Fluxtion:

1.  **Analyzes** your object graph at build time.
2.  **Derives** a topologically ordered execution plan.
3.  **Generates** a flat, optimized dispatcher.

Think of it like a **spreadsheet for streams**: when an input changes, only the affected parts of the system recompute in a fixed, predictable order. The result is a system that behaves like a **continuous decision engine** — reacting to events with predictable, replayable logic.

👉 **No runtime reactive chains**
👉 **No hidden async behaviour**
👉 **No per-event allocation**

---

## Why Fluxtion?

*   ⚡ **Ultra-Low Latency**: Compiled dispatch eliminates runtime interpretation overhead, delivering performance close to hand-written code (tens of nanoseconds for typical in-process pipelines).
*   🧠 **Deterministic Execution**: Events are processed in a fixed, topological order — no glitches, no "heisenbugs," no surprises.
*   ♻️ **Zero-Allocation Hot Path**: No framework allocation during dispatch, ensuring stable tail latency and reduced GC pressure.
*   ⏪ **Event Replay**: Exactly reproduce any production scenario by replaying input event streams for auditing, debugging, or backtesting.
*   🧩 **Less Coordination Code**: No `zip`, `merge`, or manual wiring — the execution graph is inferred automatically from your dependencies.
*   💰 **Lower Infrastructure Cost**: Higher throughput per core means fewer instances and reduced cloud spend.
*   🔒 **Secure Remote Generation**: Keep sensitive business logic in a private environment while distributing only optimized processors to clients.

---

## Quickstart (Jbang)

The fastest way to try Fluxtion is using [Jbang](https://www.jbang.dev/). Save the following as `TradeFilter.java` and run it:

```java
//REPOS fluxtion-public=https://repo.repsy.io/mvn/fluxtion/fluxtion-public
//DEPS com.telamin.fluxtion:fluxtion-builder:1.0.56
//JAVA 25

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.runtime.DataFlow;

record Trade(double price, int size) {}

void main() {
    DataFlow tradeFlow = DataFlowBuilder
            .subscribe(Trade.class)
            .map(t -> t.price() * t.size()) // compute trade value
            .filter(v -> v > 500)           // risk threshold check
            .console("large trade value: {}")
            .build();

    tradeFlow.onEvent(new Trade(100, 3));
    tradeFlow.onEvent(new Trade(100, 10)); // Prints: large trade value: 1000.0
    tradeFlow.onEvent(new Trade(55, 10));  // Prints: large trade value: 550.0
}
```

**Run:** `jbang TradeFilter.java`

---

## What it replaces

Fluxtion is not another stream processor — it replaces how you coordinate logic inside your application.

| Instead of | Use Fluxtion for |
| :--- | :--- |
| **RxJava / Reactor pipelines** | Deterministic in-process event coordination |
| **Kafka Streams (embedded)** | Ultra-low-latency, non-Kafka-bound processing |
| **Callback chains / event buses** | Structured, validated execution graphs |

---

## Compiled-at-Rest Philosophy

Unlike traditional stream processors that interpret graphs or use reflection-heavy dispatchers at runtime, Fluxtion shifts the complexity to the build phase:

1.  **Define**: Use the `DataFlowBuilder` DSL to describe your dependencies and logic.
2.  **Analyze**: Fluxtion analyzes the graph, determines the optimal execution order, and optimizes data paths.
3.  **Compile**: The graph is transformed into optimized Java source code that is compiled into your application.
4.  **Execute**: Your application runs the generated class, benefiting from native JIT optimization and zero-overhead dispatch.

---

## Ideal for
*   **Trading and pricing systems**: High-frequency execution where ordering and consistency are critical.
*   **Robotics and Control**: Real-time control loops where jitter or out-of-order execution can cause failure.
*   **AI/Agent Coordination**: Deterministic orchestration of stateful components.

---

## Standard Installation

### Maven
Import the Fluxtion BOM, then declare the artifacts you need. Generated processors
only need `fluxtion-runtime` at runtime. Local graph analysis/source generation uses
the `fluxtion-builder` build-time artifact; authoring APIs live in
`fluxtion-builder-api`.

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.telamin.fluxtion</groupId>
      <artifactId>fluxtion-bom</artifactId>
      <version>1.0.56</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependency>
  <groupId>com.telamin.fluxtion</groupId>
  <artifactId>fluxtion-builder</artifactId>
  <scope>provided</scope>
</dependency>
```

### Gradle (Kotlin DSL)
```kotlin
implementation(platform("com.telamin.fluxtion:fluxtion-bom:1.0.56"))
compileOnly("com.telamin.fluxtion:fluxtion-builder")
implementation("com.telamin.fluxtion:fluxtion-runtime")
```

---

## Documentation & Resources

*   **Docs site**: [https://telaminai.github.io/fluxtion/](https://telaminai.github.io/fluxtion/)
*   **Guides**:
    *   [Why Fluxtion?](docs/home/why-fluxtion.md) - Deep dive into core concepts.
    *   [Event Processor Model](docs/reference/event-processor-model.md) - Understanding the execution engine.
    *   [Reference Guide](docs/reference/reference-documentation.md) - Complete DSL documentation.
*   **Examples**: Check the `docs/example` folder for specific feature deep dives.

## Project Structure

Three open public artifacts:

*   **`fluxtion-runtime`** — the lightweight core runtime required to execute generated dataflows. JDK 17+ by default; agrona is a regular compile-scope transitive (agrona 2.x), so consumers targeting Java 8 / CheerpJ can exclude it and substitute `org.agrona:agrona:1.21.2` — the runtime's bytecode is binary-compatible across the two surfaces. See [`fluxtion-runtime/README.md`](fluxtion-runtime/README.md).
*   **`fluxtion-builder-api`** — the open DSL, authoring contracts, validation, replay, and annotation processors. It contains no graph-analysis or compilation engine implementation. See [`fluxtion-builder-api/README.md`](fluxtion-builder-api/README.md).
*   **`fluxtion-builder-api-all-java8`** — the Java 8-compatible authoring API/runtime bundle used by browser and embedded consumers.

## License

Fluxtion is dual-licensed under the [GNU Affero General Public License v3.0 (AGPL-3.0)](./LICENSE) and the [Server Side Public License (SSPL)](./LICENSE). Commercial licenses and support are available for enterprise use.
