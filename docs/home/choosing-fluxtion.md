# Choosing Fluxtion: when it fits and how it compares

Fluxtion is a lightweight, in‑process streaming library for Java. It shines when you need predictable, low‑latency event
processing with explicit data dependencies and minimal runtime overhead.

This page helps you decide if Fluxtion is a good fit, and contrasts it with common alternatives.

Use Fluxtion when you want

- Deterministic execution order: single‑threaded, topological dispatch with at‑most‑once invocation per node per event.
- Incremental recomputation: only affected nodes run, avoiding wasteful recalculation.
- Very low latency and small footprint: no external brokers/servers required; runs as a plain Java lib.
- Static, analyzable graphs: build once and reuse; ideal for production paths where stability matters.
- Embeddable processing: microservices, low‑GC services, edge devices, or anywhere a simple JAR is preferred over a
  platform.

You might choose another tool if you need

- Distributed scaling and state sharding across a cluster (Kafka Streams, Flink, Spark Structured Streaming).
- Complex SQL-on-streams, window joins across topics, or managed platform features out of the box.
- Dynamic operator graphs reconfigured at runtime by end users.

Comparison at a glance

- Versus reactive libraries (RxJava, Reactor): Fluxtion favors ahead‑of‑time analysis and precomputed dispatch over
  dynamic operator chains. Expect more predictability and often lower latency, with less runtime allocation and fewer
  surprises from async boundaries.
- Versus event buses/manual listeners: Fluxtion keeps your code in Java, but centralizes dependencies and execution
  order. The builder infers wiring and generates the dispatcher, reducing accidental complexity.
- Versus stream processing platforms (Flink, Kafka Streams): Fluxtion is a library, not a cluster. If you don’t need
  distributed state, checkpointing to an external store, or a managed runtime, Fluxtion keeps things simple and fast.

Typical winning use cases

- Per‑entity analytics: per user/device/symbol rolling metrics, rate limits, anomaly flags.
- Sliding‑window aggregations and counters with predictable latency.
- Real‑time risk, alerting, monitoring pipelines embedded in services.
- Deterministic pipelines that must be easy to unit test and reason about.

Operational considerations

- Deployment: ship as part of your service JAR. No external services to provision.
- Observability: add sinks to publish metrics/events; integrate with your logging/metrics stack.
- Performance: compiled graphs remove reflection and indirection. Expect fewer allocations and tight call paths.

Decision checklist

- Need sub‑millisecond in‑process responses? ✓
- Okay with single‑JVM graph and explicit integration to external IO? ✓
- Prefer explicit dependencies and predictable order over dynamic operator chains? ✓
  If you checked these, Fluxtion is likely a strong fit.

Next steps

- Learn the model: Concepts and architecture
- Try it now: 1 minute tutorial
- Build confidence: Tutorial Part‑1
