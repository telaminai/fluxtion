# Why Fluxtion: Problems it solves and the value it delivers

Fluxtion targets teams building low‑latency, predictable stream processing in Java. It replaces ad‑hoc event wiring and
complex reactive frameworks with a simple, deterministic dataflow model.

Problems Fluxtion solves

- Scattered event logic: business rules spread across listeners, queues, and schedulers make behavior hard to reason
  about. Fluxtion centralizes logic in a dependency graph with single‑pass, topological dispatch.
- Over‑recomputation: batch or imperative code often recomputes too much. Fluxtion’s incremental model only updates
  affected nodes.
- Non‑deterministic flows: callbacks and asynchronous chains can produce race conditions and surprising order. Fluxtion
  executes in a defined order with at‑most‑once invocation per node per event.
- Boilerplate plumbing: manual wiring and event bus code add risk. The builder infers dependencies and generates
  dispatch.
- Heavy runtime footprints: some stream engines require external servers or complex runtimes. Fluxtion runs in‑process
  with plain Java.

Value for your organization

- Faster delivery: developers declare dependencies; the framework handles the hard parts of dispatch and wiring.
- Lower latency: direct method invocation, no intermediate queues within the graph, minimal allocations.
- Predictability and testability: deterministic order and explicit dependencies make unit tests straightforward.
- Cost control: compiled/ahead‑of‑time graphs can be 10x faster than interpreted, reducing CPU and cloud costs.
- Flexible deployment: embed in microservices, batch jobs with near‑real‑time needs, or edge devices.

Typical use cases

- Real‑time risk and alerts (finance, ops, security)
- Metrics aggregation and anomaly detection
- Stream joins and enrichment from multiple sources
- Time/windowed analytics (moving averages, counters, rate limiting)
- Per‑entity state machines (per user, device, or symbol)

How it compares

- Versus general reactive libraries: Fluxtion favors static analysis and precomputed dispatch over dynamic operator
  graphs, yielding predictability and performance.
- Versus full stream processing platforms: Fluxtion is a library you embed, avoiding external clusters when you don’t
  need them.
- Versus hand‑rolled event buses: you keep control but gain correctness and clarity from the builder’s dependency
  analysis.

Next steps

- [Learn the basics](home/what-is-dataflow.md)
- [See the fundamentals](home/dataflow-fundamentals.md)
- Pick your intro: [for engineers](home/intro-engineers.md) or [for managers](home/intro-managers.md)
