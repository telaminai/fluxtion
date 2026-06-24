# Troubleshooting
---

A short checklist of common issues and how to fix them when building and running Fluxtion DataFlows.

## Nothing happens when I send events
- Verify you actually subscribed to the right type or key:
  - `subscribe(String.class)` for raw String events
  - `subscribe(MyEvent::field)` if you want to project a field from a POJO
- Ensure you call `build()` (functional DSL) or finish constructing the SEP before invoking `onEvent`.
- If using sinks, confirm you added a sink or a console call at the end of the chain: `.console("msg:{}")` or `.sink("id")`.

## Time windows not updating
- Windowing operators depend on time; register a clock source. At startup publish a clock event using `ClockStrategy`:
  ```java
  import static com.telamin.fluxtion.runtime.time.ClockStrategy.registerClockEvent;
  
  dataFlow.onEvent(registerClockEvent(() -> System.currentTimeMillis()));
  ```
- In tests, use a controllable clock. If you extend the provided test utilities, call `setTime(...)`, `advanceTime(...)`, or `tick()` to drive time.

## Determinism and order look wrong
- Fluxtion executes nodes in topological order with at‑most‑once invocation per node per event. If results seem stale:
  - Check each node declares its dependency via the builder chain (e.g., ensure the node that should trigger another is actually upstream).
  - Avoid side effects that bypass the graph; instead pass values through the DataFlow so dependencies are tracked.

## Logging and debugging
- Quick visibility: add `.console("msg:{}")` at the end of a chain to print values.
- For structured auditing, enable the event log auditor (see reference) and inspect the last audit record via `dataFlow.getLastAuditLogRecord()`.
- Tests include `logcaptor` and `system-rules` to capture output if you need assertions on logs.

## IDE or tests fail with IllegalAccessError
- The build config opens `jdk.internal.misc` for some dependencies via Surefire:
  - Maven Surefire already includes: `--add-opens java.base/jdk.internal.misc=ALL-UNNAMED`.
- If running in your IDE, mirror this JVM option in your run configuration if you encounter `IllegalAccess` problems.

## Java version
- Fluxtion targets Java 21. Ensure your project uses a Java 21 toolchain and your IDE is set to 21 for compiling and running examples.

## Kafka or external I/O integration
- Fluxtion is infrastructure‑agnostic. For Kafka, consume records, publish to the `DataFlow` via `onEvent`, then produce results. Start with at‑least‑once: commit offsets after successful processing. See the Integrations guide.

## High Tail Latency (p99/p99.9 spikes)
- **Identify the Bottleneck**: Use the [Monitoring Guide](../how-to/monitoring-dataflow-runtime.md) to measure node-level execution time.
- **Blocking Sinks**: Fluxtion is single-threaded by design. If a Sink performs a blocking I/O call (e.g., a slow database write), it will stall the entire event processor.
    - *Fix*: Use asynchronous sinks or internal buffering if the destination system is slow.
- **GC Interference**: While Fluxtion is zero-GC on the hot path, your own node logic or external libraries might be allocating. 
    - *Fix*: Profile with JFR (Java Flight Recorder) to identify allocation hot spots.

## Memory Leaks or OutOfMemoryError
- **Large Time Windows**: Sliding windows with many buckets or long durations can consume significant memory.
    - *Fix*: Reduce bucket count or window duration if memory is tight.
- **Unbounded Collections**: Avoid growing collections (Lists/Maps) inside nodes without a cleanup strategy.
    - *Fix*: Use bounded buffers or Fluxtion's built-in windowing/aggregation functions.

## Threading Issues and Race Conditions
- **Thread Safety**: The `DataFlow` instance is NOT thread-safe for concurrent `onEvent` calls.
    - *Fix*: Use `DataFlowConnector` to manage single-threaded delivery, or synchronize your own calls to `onEvent`.
- **External State Access**: If your nodes access shared external state, ensure that access is thread-safe or (preferably) move the state into the DataFlow graph itself.

## Mongoose Integration Issues
- **Feed Mismatch**: If events are not reaching your processor in Mongoose, verify the feed name in `subscribeToFeed("name", ...)` matches the name in your Mongoose YAML configuration.
- **Plugin Loading**: Ensure your DataFlow JAR is correctly placed in the Mongoose `plugins/` directory and that its package is scanned for discovery.

## AOT Compilation Errors
- **Missing Builder Dependency**: If you expect an AOT-optimized dispatcher but see interpreted behavior, ensure the Fluxtion builder dependency is in your build classpath (provided scope).
- **Annotation Processing**: If using the Imperative model, ensure your IDE or build tool has annotation processing enabled to detect `@OnEventHandler` and `@Start`/`@Stop` annotations.

## "cannot find matching constructor" during Fluxtion.compile
- Symptom: compile fails with
  ```
  RuntimeException: cannot find matching constructor for: Field{name=myField, fqn=com.example.MyNode, ...} failed to match for these fields:[someField]
  ```
- Cause: Fluxtion's source-gen extractor walks each live node via reflection and emits Java source that reconstructs it. Primitive / String / enum fields render as literal initialisers; complex types (`Map<...>`, `List<...>`, `Set<...>`, custom classes without a discoverable constructor) cannot be rendered as Java literals.
- Fix: tell the extractor to skip the field. Two equivalent options:
  - Annotate with `@FluxtionIgnore` (FQN: `com.telamin.fluxtion.runtime.annotations.builder.FluxtionIgnore`) — the documented Fluxtion opt-out, explicit about which serialiser is being targeted.
  - Declare the field `transient` — Fluxtion's source-gen *is* serialisation (just to source code rather than binary), so the standard Java keyword is honoured.
- Either prevents the extractor from trying to reconstruct the field. The field initialiser still runs in the generated processor's no-arg constructor, so runtime semantics are preserved:
  ```java
  @FluxtionIgnore
  private final Map<String, BookState> books = new HashMap<>();
  ```
- Primitives, Strings, and enums don't need this treatment — they serialise fine.

## "use @AssignToField to resolve clashing types"
- Symptom: compile fails with
  ```
  RuntimeException: cannot find matching constructor for: Field{name=myNode, fqn=com.example.MyNode, ...} use @AssignToField to resolve clashing types these fields:[fieldA, fieldB]
  ```
- Cause: your node has two or more constructor parameters of the same type (e.g. two `String`s, two `double`s, two same-typed references). Fluxtion's source-gen needs to emit a constructor call in the generated processor and can't decide which arg becomes which field by type alone.
- Fix: annotate each ambiguous parameter with `@AssignToField("fieldName")` (FQN: `com.telamin.fluxtion.runtime.annotations.builder.AssignToField`). The string value names the **field** the parameter binds to.
  ```java
  import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;

  public LinearLayer(InputLayer upstream,
                     @AssignToField("weightsPath") String weightsPath,
                     @AssignToField("biasPath") String biasPath) {
      this.upstream = upstream;
      this.weightsPath = weightsPath;
      this.biasPath = biasPath;
      // ...
  }
  ```
- You only need to annotate the parameters that clash. Unique-typed parameters (e.g. the `InputLayer upstream` above) bind unambiguously.
- Prefer this over renaming or merging the fields when the constructor surface matters for callers — `@AssignToField` is purely a source-gen hint and has no effect on instantiation outside the generated processor.

## Downstream `@OnTrigger` nodes never fire even though the topology looks right
- Symptom: a node was registered (`cfg.addNode(myNode, "name")`) and downstream nodes hold a field reference to it, but the downstream `@OnTrigger` methods never run. Generated source shows the downstream node's dispatch block is missing entirely (no `if (guardCheck_downstream()) downstream.method()`), or the upstream's dirty flag is declared but never set to `true`.
- Cause: the registered node has **no trigger-style annotation**. A node only propagates change downstream if it has at least one of:
  - `@OnEventHandler` — entry point for an external event class
  - `@OnTrigger` — fires after upstream nodes update
  - `@AfterEvent` — fires once at end of each event cycle

  A plain POJO with only setters/getters can be registered, and downstream nodes can structurally depend on it, but the dispatcher walks `@OnEventHandler` / `@OnTrigger` / `@AfterEvent` chains — not arbitrary setter calls. Without one of these annotations, the POJO's dirty flag is never set, so downstream guards always evaluate false.
- Common case: bridging a DSL chain into imperative `@OnTrigger` downstream nodes. The wrong shape:
  ```java
  // ❌ POJO setter target — downstream tools never fire
  PlannerState plannerState = new PlannerState();   // plain POJO, no annotations
  DataFlowBuilder.subscribeToNode(classifier)
      .map(...).filter(...).map(...)
      .push(plannerState::setCurrentCall);          // setter call bypasses dispatcher
  ```
  The fix: use `.flowSupplier()` as the DSL terminal and pass the resulting `FlowSupplier<T>` into a state-holder node that has its own `@OnTrigger`:
  ```java
  // ✅ FlowSupplier as DSL terminal; PlannerState participates in dispatch
  FlowSupplier<ToolCall> plannedCall = DataFlowBuilder
      .subscribeToNode(classifier)
      .map(QueryClassifier::getCurrentIntent)
      .filter(Planner::isActionable)
      .map(Planner::toToolCall)
      .flowSupplier();

  PlannerState plannerState = new PlannerState(plannedCall);

  // In PlannerState:
  //   private final FlowSupplier<ToolCall> upstream;
  //   @OnTrigger
  //   public boolean onPlannerUpdate() {
  //       currentCall = upstream.get();
  //       return currentCall != null;
  //   }
  ```
  The `FlowSupplier` IS a triggering node (the DSL chain's tail), so `PlannerState` has a real upstream to drive its `@OnTrigger`, and downstream imperative nodes that hold a `PlannerState` field now fire correctly.
- Rule of thumb: `.push()` is for side effects observable outside the graph (logging, metric emission, external sinks). `.flowSupplier()` + an `@OnTrigger` holder is for in-graph state propagation that imperative nodes need to react to.

## "cannot find symbol: method lambda$null$..." in DSL-flow generated code
- Symptom: compile of generated processor fails with
  ```
  Processor.java:N: error: invalid method reference
    cannot find symbol: method lambda$null$abc12345$1()
    location: class com.example.Main
  ```
  …pointing at a `MapRef2RefFlowFunction`, `FilterFlowFunction`, or `PushFlowFunction` field initialiser.
- Cause: the DSL was passed a lambda that captures state (e.g. `c -> c.getCurrentIntent()` or `call -> myNode.setSomething(call)`). Inside `Fluxtion.compile(cfg -> { DataFlowBuilder...; })` the source-gen reflects over each `SerializableFunction` / `SerializableConsumer` and emits a method-ref expression in the generated processor's field initialisers. Captured lambdas compile to javac-synthesised `lambda$null$<hash>$<n>` methods that only exist as bytecode — Fluxtion tries to reference them by name in generated source and the symbol doesn't resolve.
- Fix: replace the lambda with a **named method reference**. Three shapes work:
  - **Static method ref** — `Planner::isActionable`. For pure transforms with no captured state.
  - **Unbound instance method ref** — `QueryClassifier::getCurrentIntent`. For reading a property of the upstream type in `.map(...)`. Equivalent to `c -> c.getCurrentIntent()` but renders to a stable symbol.
  - **Bound instance method ref** — `plannerState::setCurrentCall`. For pushing into a node already in the graph. Source-gen emits this as a direct `instance.method(...)` call (no invokedynamic at runtime), so it works under all backends including CheerpJ.

  ```java
  DataFlowBuilder
      .subscribeToNode(classifier)
      .map(QueryClassifier::getCurrentIntent)   // unbound
      .filter(Planner::isActionable)            // static
      .map(Planner::toToolCall)                 // static
      .push(plannerState::setCurrentCall);      // bound
  ```
- When you genuinely need lambda capture: move the consumer outside the compile boundary via `.sink("name")` and register the lambda at runtime with `flow.addSink("name", lambdaWithCapture)`. Caveat: sink callbacks fire externally to the graph dispatch, so downstream Fluxtion nodes won't see the effect — only use sinks for side effects, not for in-graph state updates that other nodes depend on.
- Note the contrast with the runtime-only DSL path (`DataFlowBuilder...build()`): there, lambdas are the *required* shape because no source-gen happens; bound method refs trip CheerpJ's invokedynamic resolution. The rules invert depending on which compile API you use.

## "cannot find symbol" referencing my user types in generated code
- Symptom: javac fails on the generated processor with errors like `cannot find symbol: class Order` or `cannot find symbol: class MyNode`, even though those classes clearly exist in your project.
- Cause: your user types are in the unnamed (default) package. The Java Language Specification forbids classes in named packages from referencing default-package types. Fluxtion generates its processor in a derived named package and cannot see your default-package classes.
- Fix: declare a named package on every Java file involved — `package com.example;` is the conventional choice. Apply to all event types, processor nodes, and the entry-point Main; no other code change required.

## State persists across `flow.init()` calls
- Symptom: when you reuse one processor instance across multiple event batches (e.g. driving a replay loop), accumulated state from the previous batch leaks into the next — counts double, maps grow, totals never reset.
- Cause: `flow.init()` resets per-event Fluxtion machinery (dirty flags, queue state) but does **not** touch user-node fields.
- Fix options, in order of preference:
  1. Add an `@Initialise` lifecycle method on the node that clears its state — the Fluxtion-idiomatic answer.
  2. Add an explicit `reset()` method on the node and call it after `flow.init()` — clearest in teaching code or where the reset semantics are non-trivial.
  3. Construct a fresh processor via `((CloneableDataFlow<?>) template).newInstance()` — strongest reset (new node instances, fresh closures, fresh sinks). Costs one `Processor.<init>` per batch; no recompile.

## `flow.getNodeById(...)` fails to compile (NoSuchFieldException)
- Symptom: javac error `unreported exception NoSuchFieldException; must be caught or declared to be thrown`.
- Cause: `DataFlow.getNodeById(String)` declares a checked `NoSuchFieldException` for the case where the requested name doesn't resolve to a node.
- Fix: declare `throws NoSuchFieldException` (or the wider `throws Exception`) on the calling method, or wrap the call in a try/catch. The exception can in practice only fire if the name string doesn't match a node — most callers can safely propagate.

## Which compile shape should I use? `compile` vs `compileDispatcher` vs `addNode(node, "name")`
- Three legitimate APIs, each serving a distinct need. Pick by what your caller does with the node instance after compile.
- **`Fluxtion.compile(c -> c.addNode(node))`** — generated processor instantiates a fresh internal node. Your reference is a bystander; the processor-managed instance can't be retrieved without a name. **Right for fire-and-forget production deploy** with no introspection needed; minimal generated code surface.
- **`Fluxtion.compile(c -> c.addNode(node, "name"))`** + `flow.getNodeById("name")` — same baked-instance shape as above, but the processor-managed instance is retrievable by name. **Right for production code that needs id-keyed state queries** (inspection, monitoring) without binding a live-reference contract.
- **`Fluxtion.compileDispatcher(c -> c.addNode(node))`** — sets `dispatchOnlyVersion=true`; the generated `NodeDispatchTable`'s member fields are populated **after** construction via `assignMembers(instanceMap)`. The user reference passed in is bound; the dispatcher fires into the same instance. **Right for tests, debugging, and demos** where holding the reference is more ergonomic than a name lookup.
- All three round-trip through the same compiler; only the generated shape and instance-wiring differ. Symptom of having picked the wrong one: assertions on `myNode.someField` read the initial value (zero / empty) even after events have fired — that's the bystander shape; switch to one of the live-binding options.

## Still stuck?
- Check the Examples page and Reference guide for a code snippet matching your use case.
- Open an issue with a minimal reproducer on GitHub if the behavior still surprises you.
