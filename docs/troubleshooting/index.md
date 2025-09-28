# Troubleshooting

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

## Still stuck?
- Check the Examples page and Reference guide for a code snippet matching your use case.
- Open an issue with a minimal reproducer on GitHub if the behavior still surprises you.
