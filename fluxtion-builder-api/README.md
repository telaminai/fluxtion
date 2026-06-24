# fluxtion-builder-api

Open authoring API for Fluxtion graphs.

This artifact contains the DSL, graph-authoring contracts, compile configuration
value types, validation/replay helpers, annotation processors, and user extension
SPIs. It intentionally does not contain the local graph-analysis or source
generation engine.

Use `fluxtion-builder-api` when you need to compile code that describes Fluxtion
graphs but you do not need to run local generation in that module.

## Typical dependency layout

Prefer the BOM so the open runtime/API artifacts and the local build-engine
artifact stay on a tested version pairing.

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
```

Authoring-only module:

```xml
<dependency>
    <groupId>com.telamin.fluxtion</groupId>
    <artifactId>fluxtion-builder-api</artifactId>
</dependency>
```

Runtime module for generated processors:

```xml
<dependency>
    <groupId>com.telamin.fluxtion</groupId>
    <artifactId>fluxtion-runtime</artifactId>
</dependency>
```

Module that performs local builds:

```xml
<dependency>
    <groupId>com.telamin.fluxtion</groupId>
    <artifactId>fluxtion-builder</artifactId>
    <scope>provided</scope>
</dependency>
```

`DataFlowBuilder.build()` discovers the build engine through
`DataFlowBuildService`. If only `fluxtion-builder-api` is present, build-time
terminals report an actionable missing-provider diagnostic. Running a
pre-generated processor does not need the build engine; it only needs
`fluxtion-runtime`.

## Java 8 / browser use

Use `fluxtion-builder-api-all-java8` for Java 8 or browser authoring. Pair it
with the matching Java 8 build-engine artifact only in environments that perform
generation in-process.
