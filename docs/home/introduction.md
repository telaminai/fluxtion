# Introduction to Fluxtion

Fluxtion is a Java library and builder for real-time, in‑memory dataflow processing. It helps you express business logic
as a directed graph of small, composable functions and stateful components that update incrementally when new events
arrive.

Think of it like a spreadsheet for streams: when an input changes, only the dependent cells recompute. Fluxtion brings
this incremental, dependency-driven update model to event streams, enabling predictable, low‑latency processing with
clear dependencies and excellent performance.

Highlights

- DataFlow programming model: declare how values depend on each other, not when to recompute.
- Deterministic execution: events propagate through a topologically sorted graph.
- Low overhead: avoids generic reactive machinery; directly invokes methods in dependency order.
- Scales down and up: great for microservices, trading systems, IoT gateways, or embedded analytics.
- Familiar Java: plain objects and methods; no special runtime server required.

Core building blocks

- Events: any Java object submitted into the DataFlow.
- Nodes: functions or stateful components wired together by dependencies.
- Graph: a DAG computed by the builder that determines dispatch order.
- Handlers and triggers: annotated methods that receive events or fire when dependencies update.

Where Fluxtion fits

- Real‑time analytics and monitoring
- Complex event processing and enrichment
- Per‑key aggregations (counts, moving windows)
- Signal generation and alerting
- Incremental computation pipelines

See also

- [DataFlow fundamentals](home/dataflow-fundamentals.md)
- [Why Fluxtion](home/why-fluxtion.md)
- [What is DataFlow programming](home/what-is-dataflow.md)
- [Intro for engineers](home/intro-engineers.md)
- [Intro for managers](home/intro-managers.md)
