# Why Fluxtion exists
---

Fluxtion is a deterministic execution engine for low-latency, event-driven systems.

It replaces ad-hoc event wiring and complex reactive pipelines with a simple, compiled execution model.

## The problem: The "Plumbing Tax"

Most event-driven systems pay a hidden cost in complexity and performance:

- **Scattered event logic**: Business rules spread across listeners, queues, and schedulers make behavior hard to reason about.
- **Over‑recomputation**: Batch or imperative code often recomputes too much. Traditional models struggle to update only what changed.
- **Non‑deterministic flows**: Callbacks and asynchronous chains can produce race conditions and surprising order, making debugging a nightmare.
- **Boilerplate plumbing**: Manual wiring and event bus code add risk and cognitive load, distracting from the actual business logic.

As systems grow, this coordination logic often dominates both performance and developer effort.

Reactive frameworks focus on asynchronous composition. Fluxtion focuses on deterministic execution.

## The missing layer: Deterministic Coordination

Fluxtion introduces a **deterministic coordination layer** that automates the "plumbing" of your application. Instead of manually wiring components, you define what depends on what, and Fluxtion handles the execution.

This transforms your application into a **continuous decision system** — where events trigger state updates and decisions in a single, deterministic execution pass.

## The shift: From Runtime to Compile Time

The core innovation of Fluxtion is moving coordination from **runtime → compile time**.

- **Execution Derived**: The compiler analyzes your object graph and derives a topologically ordered execution plan.
- **Dispatcher Generated**: Fluxtion generates a flat, optimized dispatcher that uses direct method calls instead of dynamic dispatch or reflection.
- **Coordination Removed**: Because the execution path is precomputed, there is no runtime graph traversal or coordination overhead.

## What you get

- **Determinism**: Events are processed in a fixed, topological order — no glitches, no surprises.
- **Fewer Coordination Bugs**: Eliminates glitches, ordering issues, and hidden re-subscriptions common in reactive systems.
- **Replayability**: Because execution is deterministic, persisting the input stream allows for exact replay and state reconstruction.
- **Ultra-Low Latency**: Compiled dispatch eliminates runtime interpretation overhead, delivering sub-microsecond performance (often tens of nanoseconds for typical in-process pipelines).
- **Less Code**: No need for `zip`, `merge`, `share`, or manual coordination logic. The execution graph is inferred automatically.
- **Simpler Mental Model**: Think in terms of "what depends on what" instead of reasoning about event flow, subscriptions, and execution order.
- **Infrastructure Independence**: Fluxtion is not tied to any specific messaging or streaming platform; business logic remains decoupled from the transport or compute stack.
- **Economic Value**: Substantial throughput per core means fewer servers and lower cloud costs.
- **Faster Time-to-Market**: Developers focus purely on business rules; Fluxtion manages the complexity of coordination and execution.

## Determinism, audit, and operational simplicity

Fluxtion executes event-driven systems with a fixed, deterministic schedule. The same inputs always produce the same execution path and outputs, providing a foundation for operational excellence:

- **Reproducible debugging**: Eliminate "heisenbugs" by replaying the input stream to recreate the exact execution state.
- **Audit trails**: Explain exactly how and why a decision was made by following the deterministic graph propagation.
- **Deterministic regression testing**: Verify behavior with perfect consistency, removing flaky tests caused by race conditions.
- **Explainable decision systems**: Essential for regulated industries where transparent logic is a mandatory requirement.

## Where Fluxtion fits best

Fluxtion is a specialized engine designed for high-stakes, low-latency environments:

- **Trading and Pricing**: High-frequency execution where ordering, consistency, and sub-microsecond latency are critical.
- **Fraud and Risk Detection**: Real-time evaluation of complex dependencies where a single missed event or race condition can be costly.
- **Robotics and Edge Control**: Predictable control loops where jitter or out-of-order execution can lead to physical failure.
- **Telecom and Signaling**: High-throughput message processing where per-core efficiency directly impacts infrastructure costs.
- **AI/Agent Orchestration**: Coordinating multiple stateful components in a deterministic way to prevent emergent, unpredictable behavior.

## When to use Fluxtion

Fluxtion is a strong fit when:

- **Deterministic execution over async composition**: Fluxtion focuses on predictable execution order rather than asynchronous scheduling. It complements async frameworks rather than replacing them.

- **Latency matters**: You need sub‑millisecond (or sub-microsecond) in‑process responses.
- **Deterministic execution is required**: You need to guarantee the order of operations and avoid race conditions.
- **Coordination complexity is high**: You have many interdependent stateful nodes that are hard to wire manually.
- **Replacing complex RxJava/Reactor pipelines**: You want to move from hard-to-debug reactive chains to a deterministic execution model.

## When NOT to use Fluxtion

You may prefer other tools if you need:

- **Distributed scaling**: If you need to shard state across a cluster, tools like **Apache Flink** or **Spark Structured Streaming** are better suited.
- **Kafka-native pipelines**: If your primary need is managed state recovery via Kafka topics, **Kafka Streams** is the standard choice.
- **SQL-first analytics**: If you want to use SQL to query streams, consider a dedicated stream processing platform.
- **Dynamic runtime graphs**: If you need to reconfigure the processing logic at runtime without a recompile.

---

Fluxtion replaces the most complex part of event-driven systems — coordination — with a deterministic, compiled execution model.

## Integration with Mongoose

Fluxtion focuses on deterministic execution logic, while [Mongoose Server](https://telaminai.github.io/mongoose/) provides the production-ready runtime around it—handling feeds, threading, lifecycle, and operational control. Mongoose's [plugin extension architecture](https://telaminai.github.io/mongoose/overview/plugin_extension_architecture/) keeps integration code in separate plugins that can be tested and combined at runtime.

👉 [Learn more about running Fluxtion with Mongoose](fluxtion-with-mongoose.md)  
👉 [Start the Mongoose + Fluxtion tutorial](../getting-started/run-fluxtion-in-mongoose.md)

## Still have questions?

Fluxtion introduces a different execution model, and that raises important questions:

- Why not just use C++ or Rust?
- How does recovery work?
- Does AOT make systems rigid?

👉 [Read the Fluxtion FAQ: Common questions and tradeoffs](../reference/faq.md)
