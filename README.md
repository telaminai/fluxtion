# Fluxtion – Dataflow processing for Java

[![Maven Central](https://img.shields.io/maven-central/v/com.telamin.fluxtion/fluxtion-builder.svg)](https://search.maven.org/search?q=g:com.telamin.fluxtion)
![Java](https://img.shields.io/badge/java-21+-blue)
[![License](https://img.shields.io/badge/license-AGPL%2FSSPL-important)](./LICENSE)
[![Docs](https://img.shields.io/badge/docs-website-blue)](https://telaminai.github.io/fluxtion/)

Fluxtion is a lightweight Java library for real‑time, in‑memory dataflow processing. You declare how values depend on each other; Fluxtion builds a dependency graph and executes updates deterministically when events arrive.

- Deterministic: topologically ordered dispatch, at‑most‑once per node per event
- Low latency: direct method invocation, minimal allocations
- Familiar Java: plain objects and methods; embed in any app

## Quick links
- Docs site: https://telaminai.github.io/fluxtion/
- GitHub: https://github.com/telaminai/fluxtion

## Install
Add the builder (brings in the runtime):

Maven
```xml
<dependency>
  <groupId>com.telamin.fluxtion</groupId>
  <artifactId>fluxtion-builder</artifactId>
  <version>0.9.6</version>
</dependency>
```

Gradle (Kotlin DSL)
```kotlin
implementation("com.telamin.fluxtion:fluxtion-builder:0.9.3")
```

Requires Java 21 (toolchain), builds with Maven Wrapper.

## Quickstart
```java
import com.telamin.fluxtion.runtime.DataFlow;

public class HelloFluxtion {
    public static void main(String[] args) {
        DataFlow dataFlow = DataFlowBuilder
                .subscribe(String.class)           // accept String events
                .map(String::toUpperCase)          // transform
                .console("msg:{}")                 // print to console
                .build();                          // build the DataFlow

        dataFlow.onEvent("hello");  // prints: msg:HELLO
        dataFlow.onEvent("world");  // prints: msg:WORLD
    }
}
```

More examples and reference:
- [Why Fluxtion](docs/home/why-fluxtion.md)
- [Dataflow fundamentals](docs/home/dataflow-fundamentals.md)
- [Reference guide](docs/reference/reference-documentation.md)

## Modules
- fluxtion-runtime – Core runtime for executing graphs
- fluxtion-builder – Builder DSL for constructing dataflows



