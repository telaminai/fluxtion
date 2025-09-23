# Fluxtion for engineers

This section gives a practical, technical view of Fluxtion for Java developers.

What you build

- A DataFlow: a generated component that exposes event endpoints (e.g., onEvent) and service methods.
- Nodes: POJOs you write that contain logic. You wire them via builder DSL or annotations.
- Graph: the builder analyzes nodes and dependencies to produce a topologically ordered DAG with precomputed dispatch.

Execution model

- You submit events (any Java object). Handler methods receive them.
- Each handler returns a boolean to indicate whether downstream dependents should be triggered.
- The runtime invokes dependent nodes’ trigger methods once all their parents have completed.
- Each node runs at most once per event, in deterministic topological order.

Performance characteristics

- In‑process, zero external broker required.
- Direct method calls (no reflective hot path in compiled mode), minimal allocations.
- Optional ahead‑of‑time (AOT) code generation can yield ~10x over interpreted builder mode, lowering latency and CPU.

Developer workflow

- Define domain events (simple classes) and stateful components.
- Use the builder DSL to subscribe to streams, filter/map/group, and connect to sinks; or use imperative/annotation
  style.
- Write unit tests using JUnit/Hamcrest. Many examples extend MultipleSepTargetInProcessTest for driving events and
  asserting outputs.
- Deploy as a library inside your service. No cluster or special runtime required.

When to choose Fluxtion

- You need deterministic, low‑latency dataflow inside a Java service.
- You want explicit control and visibility over the graph and state.
- Your workload benefits from incremental updates (aggregations, joins, windows, per‑key state machines).

Interoperability

- Works with your existing messaging (Kafka, MQ, HTTP) at the edges—you integrate adapters to feed events in and publish
  out.
- Pure Java 21 codebase; no container dependencies. Shaded Agrona in runtime avoids conflicts.

Learn next

- Fundamentals: home/dataflow-fundamentals.md
- DSL overview: reference/functional/overview-functional.md
- DSL API reference: reference/functional/dataflow-functional-dsl.md
- Imperative/POJO integration: reference/imperative/agent_integration.md
