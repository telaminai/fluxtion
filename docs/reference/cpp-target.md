# C++ target: what works today

!!! warning "Preview"
    The C++ target is a **preview**. It has not been through the review gate that the Java target has.
    Do not assume it behaves like Java: check each construct you use against this page, and compare the
    output with a Java build of the same graph (see [How it is checked against Java](#how-it-is-checked-against-java)).

!!! info "Which release this describes"
    This page describes the Fluxtion compiler at release **1.0.79** (`fluxtion-builder` 1.0.79, built
    against `fluxtion-runtime` 1.1.1). It was checked against that release's source and its C++ test suite
    on 2026-10-17. If you use a later release, check this page against it.

## What it is

- The generator can emit **C++ source** for a graph instead of Java. You choose the language for each
  compile with `FluxtionCompilerConfig.setTargetId("cpp")`. If you leave it unset, the output is Java.
- The *route* is a separate setting, `sourceGeneratorId` (`"local"` or `"remote-http"`). It decides
  where generation runs, not which language you get back.
- The graph you write does not change. It uses the same annotated nodes, the same DSL and the same
  `EventProcessorConfig`.
- The output is one generated processor header. It compiles against a **header-only C++ runtime**
  (`fluxtion.h` and the headers it includes), so there is nothing to link. You need C++17: the
  generated header stops with an `#error` under anything older.

```java
EventProcessorFactory.compile(
        cfg -> DataFlowBuilder.subscribe(Tick.class)
                .mapToInt(Tick::getPrice)
                .aggregate(IntSumFlowFunction::new).id("total"),
        compilerConfig -> {
            compilerConfig.setTargetId("cpp");          // the language
            compilerConfig.setPackageName("com.example.pricing");
            compilerConfig.setClassName("PricingProcessor");
            compilerConfig.setOutputDirectory("target/generated-cpp");
            compilerConfig.setResourcesOutputDirectory("target/generated-cpp");
            compilerConfig.setWriteSourceToFile(true);
            compilerConfig.setCompileSource(false);     // there is no javac step for C++
            compilerConfig.setFormatSource(false);      // the Java formatter cannot format C++
        });
```

The output file is named after the class with a **`.java` extension**, but it contains C++. Rename it
to `.h` or `.hpp` when you build. The Fluxtion Maven plugin has no setting for the target, so select
it in code as shown above.

## How you obtain C++ output today

Read this section before you plan around the C++ target. Not every part of the C++ toolchain is
publicly published.

| piece | what it does | where it is published |
|---|---|---|
| `fluxtion-builder` (1.0.67 or later) | builds the graph and sends it to a generator, carrying `targetId` | the public Repsy repository `https://repo.repsy.io/mvn/fluxtion/fluxtion-public`. Maven Central carries only an older line (1.0.9) without the C++ target. |
| `fluxtion-generator-cpp` | the C++ emitter, used when generation runs **locally**. Its jar also contains the header-only runtime. | **not public**: it is in neither Maven Central nor the public Repsy repository |
| `fluxtion-serialiser-cpp` | maps `Duration` and `File` fields to C++ (see [captured node state](#captured-node-state)) | **not public** |
| the HTTP generator | generates on a server. Since 1.0.67 the server build includes the C++ emitter and answers a request for `targetId` `"cpp"`. | a hosted service, reached with the API key described in the [AOT quick start](../home/quickstart-aot.md) |

So, today:

- **Local generation** needs `fluxtion-generator-cpp` on the classpath, and that artefact is not public.
- **Remote generation** needs only the public `fluxtion-builder` on your side. If no local generator is on the
  classpath, the builder sends the graph to the configured remote generator and passes `targetId` along.
  You can also choose that route explicitly:

  ```java
  compilerConfig.setSourceGeneratorId("remote-http");   // where generation runs
  compilerConfig.setTargetId("cpp");                    // which language comes back
  ```

  A server without the C++ emitter refuses the request and lists the ids it supports.
- **The remote route returns only the processor source.** The header-only runtime it includes is
  packaged inside `fluxtion-generator-cpp`, which you write out with
  `CppSourceGenerator.writeRuntimeHeaders(dir)`. The server does not send it.

!!! note "To be confirmed"
    This page does not yet say whether the public hosted service answers `targetId` `"cpp"`, or how a
    user without access to `fluxtion-generator-cpp` gets the runtime headers. Until it does, treat C++
    output as available only where both the generator and the runtime are available to you.

## What you write, and what is generated

- **Generated**: the dispatch order, guards, dirty flags and the processor's lifecycle calls. For the DSL,
  each flow node is generated with its framework behaviour built in.
- **Yours**:
    - **Node bodies.** The generator declares a stub struct for each of your node classes, and you write
      the callbacks out of line. Each DSL method reference becomes a function declaration that you
      implement. Reference values reach these functions as `const void*`, which you cast to your type.
    - **The driver** that feeds events in. A processor has a typed handler method for each event type, for example
      `handle_Tick(Tick*)`. For an application shape, `fluxtion::DataFlowConnector` polls sources on one
      thread.
    - **Your event types**, in a `<YourProcessor>.types.h` header that the generated file names and
      includes. The generator knows your event types by name, not by layout. It checks each numeric
      field's width with a `static_assert`. It does not check booleans, strings or enums, or field order,
      meaning and units. The dispatch key is the fully qualified **Java** class name, so if you rename the
      Java class, the C++ struct still compiles but the processor stops recognising the event.

### Captured node state

When the graph is built, the state your nodes hold is carried into the C++ processor, or the build is
refused. It is never dropped silently.

| Java field | C++ |
|---|---|
| `boolean`, `char`, `byte`, `short`, `int`, `long`, `float`, `double`, `String` | a member initialised to the configured value |
| an array, or a list or set of one scalar kind | the values as data at namespace scope, plus a `set_<field>(const T* data, size_t count)` setter on the stub that you implement and the processor calls before `init()`. You choose the container. |
| `Duration`, `File` | `std::chrono::nanoseconds` and `std::filesystem::path`, **only if `fluxtion-serialiser-cpp` is on the classpath that builds the graph**. The mapping runs where the graph is built, not on the generator. |
| an **enum** | **refused by name**, as below |
| a `Map`, a mixed-element or object collection, `java.time` values, any other object | **refused by name**, as below |
| `transient` or `@FluxtionIgnore` fields | not carried. Initialise them yourself in `@Initialise`. |

A refusal names the node, the field and its type. To carry such a field, register a
`FieldToSourceSerializer` whose `language()` returns `"cpp"`. It returns the member declaration without
the trailing semicolon, and names the member with `FieldContext.getFieldName()`. Register it through
`META-INF/services` on the classpath that **builds** the graph. If the field is not configured state,
mark it `transient` or `@FluxtionIgnore` instead.

!!! note "Enums are refused"
    An enum field is refused today unless a `"cpp"` serialiser is registered for it. The refusal message
    currently says that "scalars, strings and enums cross", which is wrong for enums.

## Supported subset

### Annotations (event-time behaviour)

Each of these is emitted, and each has a test in the generator's C++ suite that compiles and runs it:

- `@OnEventHandler`, including `filterId`, `filterString` and `propagate = false`;
- `@OnTrigger`, `@OnParentUpdate`, `@AfterTrigger` and `@AfterEvent`;
- `@TriggerEventOverride`, `@NoTriggerReference` and `@PushReference`;
- the lifecycle annotations `@Initialise`, `@Start`, `@StartComplete`, `@Stop` and `@TearDown`;
- `@OnBatchPause` and `@OnBatchEnd`;
- `@ExportService`.

Build-time annotations such as `@AssignToField`, `@Config`, `@ExcludeNode` and `@FluxtionIgnore` shape
the graph before emission, so they need nothing from the target.

!!! warning "Do not use `@Inject` on the C++ target"
    `@Inject` is not supported, and the build does **not** refuse it:

    - **An injected node of your own type** is created and dispatched in the right order, but the node
      that injected it gets no reference to it. Its C++ stub has no member for the field.
    - **An injected framework service**, such as `NodeNameLookup`, `DirtyStateMonitor` or `Clock`, does not
      appear on the stub either.

    Pass parents as ordinary constructor or field references instead. Those are carried, as typed
    `p0`, `p1`, … members.

### DSL

The generator's capability matrix builds one graph per construct. It records **36 constructs emitted
and 2 refused**:

- **Emitted**:
    - `map` (reference to `int`, `double`, `long` or reference, and `int`, `double` and `long` to
      themselves), `filter` (`int`, `double`, `long` and reference) and `push`;
    - `mapBiFunction` (binary map) and `defaultValue`;
    - `aggregate` with sum, min, max, average and identity over `int`, `double` and `long`, and `count`;
    - the publish, reset and update triggers;
    - windows: tumbling, sliding (invertible and non-invertible) and fixed-size by count;
    - `flatMap`, `groupBy`, `peek`, `notify`, `merge`, and `mapOnNotify` to a node registered with
      `addNode`.
- **Refused**:
    - `toList` and the other generic aggregates that read their input. Your type reaches C++ as
      `const void*`, so there is no C++ type to hand to the aggregate.
    - `mapOnNotify` with an inline target. Nothing registers that target, so there is no node to fire.

A refused graph fails the build and names the node. Use **method references**, not inline lambdas. The
compiled path needs a method reference to emit, on Java as well.

### Processor surface

Generated in **both Java and C++**:

- the full lifecycle, including `startComplete`, `batchPause` and `batchEnd`;
- a typed handler method for each event type, and `bufferEvent` and `triggerCalculation` when the build profile enables
  buffer-and-trigger;
- exported service methods;
- `newInstance`, `getNodeById`, `lookupInstanceName`, and `isDirty` and `setDirty` when the graph has
  dirty flags;
- the audit record, `setLogSink` and the clock strategy;
- the unknown-event handler;
- the subscription manager and typed event feeds, when the profile keeps subscriptions;
- forked (parallel) triggers.

**Java only**:

- `dirtySupplier` and `getLastAuditLogRecord`;
- `registerService` on the processor. The C++ `fluxtion::DataFlowConnector` has its own typed service
  registry;
- `DataFlowContext` parameters and instance injection;
- `getDescriptor`, `getAuditorById`, `getInstanceById` and `callbacksPending`.

## Audit logs

- The C++ runtime writes **FLXA**, the [binary audit format](flxa-format.md), through `fluxtion_writer.h`.
  The Java `BinaryLogReader`, `AuditLogTool` and the audit log analyser all read it.
- **Binary only.** The C++ target writes no text audit record.
- **Settings are fixed at generation.** The C++ target has no runtime record swap and no live setter,
  and `recordEndTime` is fixed at generation (see [performance profiles](performance-profiles.md)).
- **You state the time unit.** The C++ `BinaryLogWriter` writes unit `1` (epoch milliseconds) unless
  you pass another. Pass `2` (epoch nanoseconds) when the processor runs on `SystemNanoClock`. The C++
  writer does not validate the value, so it is up to you to state it correctly.

## How it is checked against Java

- **The behaviour.** An oracle runs the same events through the Java and C++ builds of one graph. It
  compares their binary audit logs entry for entry, values and timestamps included. There are 22 such
  tests, covering every emitted DSL construct. At 1.0.79 the generator's C++ suite (71 tests, run with
  clang++ on 2026-10-17) passes in full.
- **The writer.** The compiler's Java-to-C++ audit parity tests hold the C++ FLXA writer to the Java
  writer. It is **not yet** held to the FLXA conformance corpus byte for byte.
- **A check you can run yourself.** The public
  [latency kit](https://github.com/telaminai/fluxtionauditlog-analyser/tree/main/tools/bench/latency-kit/cpp)
  requires its C++ and Java arms to print the same checksum (`conv v=120.5988`, `ladder v=2185441000`).
  Building it needs `fluxtion-generator-cpp` installed locally.

## Known limits

- **A reset trigger does not reset a tumbling window** in C++; Java does. The window still publishes on
  the reset, but its accumulator is cleared only when the window rolls.
- **The C++ FLXA writer accepts any time-unit value.** It has not been run against the
  [conformance corpus](flxa-format.md). Unit-code validation, the id-exhaustion and oversize-name
  execution tests, and by-name id translation are still open.
- **Your event-type header is not verified** beyond the width checks.
- **`@Inject` is not refused.** See [Annotations](#annotations-event-time-behaviour).
- **Unsupported node state is refused, not dropped**, as listed under
  [captured node state](#captured-node-state).
- **The latency kit's C++ arms predate array-state setters**, so a fixture that holds array state needs
  setters the kit does not implement (see [performance](performance.md)).

## Before you depend on it

1. Confirm you can get both the generated source and the runtime headers. See
   [How you obtain C++ output today](#how-you-obtain-c-output-today).
2. Check every annotation, DSL construct and captured field you use against the lists above. Avoid
   `@Inject`.
3. Build the same graph for Java and for C++, run the same events through both on a data-driven clock,
   and compare the FLXA logs in the audit log analyser.
4. Check this page against the release you use. It describes compiler release 1.0.79.
